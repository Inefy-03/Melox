package com.melox.player.data.update

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.Settings
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.File

internal enum class InstallResult { Installed, InstallerOpened, PermissionRequired }

internal suspend fun installUpdate(context: Context, apk: File): InstallResult {
    val canUseShizuku = runCatching {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
    if (canUseShizuku && installWithShizuku(context, apk)) return InstallResult.Installed
    if (!context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        return InstallResult.PermissionRequired
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    })
    return InstallResult.InstallerOpened
}

private suspend fun installWithShizuku(context: Context, apk: File): Boolean {
    val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuUpdateService::class.java))
        .processNameSuffix("update_installer")
        .daemon(false)
    val binder = CompletableDeferred<IBinder?>()
    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder.complete(service)
        }
        override fun onServiceDisconnected(name: ComponentName) {
            binder.complete(null)
        }
    }
    return try {
        Shizuku.bindUserService(args, connection)
        val service = withTimeoutOrNull(5_000) { binder.await() } ?: return false
        withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(INSTALL_DESCRIPTOR)
                    fd.writeToParcel(data, 0)
                    if (!service.transact(INSTALL_TRANSACTION, data, reply, 0)) return@use false
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    } finally {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
    }
}

private const val INSTALL_DESCRIPTOR = "com.melox.player.update.Install"
private const val INSTALL_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION
private const val DESTROY_TRANSACTION = 16_777_115

/** Loaded by Shizuku in its own process. The APK is supplied as a read-only descriptor. */
@Keep
class ShizukuUpdateService(private val context: Context) : Binder() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == DESTROY_TRANSACTION) {
            if (getCallingUid() != Process.myUid() && getCallingUid() != context.applicationInfo.uid) {
                throw SecurityException("Invalid installer caller")
            }
            Process.killProcess(Process.myPid())
            return true
        }
        if (code != INSTALL_TRANSACTION || reply == null) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(INSTALL_DESCRIPTOR)
        if (getCallingUid() != context.applicationInfo.uid) throw SecurityException("Invalid installer caller")
        val fd = ParcelFileDescriptor.CREATOR.createFromParcel(data)
        val result = runCatching {
            ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
                val size = fd.statSize
                check(size > 0)
                val process = ProcessBuilder("/system/bin/pm", "install", "-r", "-S", size.toString(), "-")
                    .redirectErrorStream(true)
                    .start()
                try {
                    process.outputStream.use { input.copyTo(it) }
                    process.inputStream.bufferedReader().use { it.readText() }
                    process.waitFor() == 0
                } finally {
                    process.destroy()
                }
            }
        }.getOrDefault(false)
        reply.writeNoException()
        reply.writeInt(if (result) 1 else 0)
        return true
    }
}
