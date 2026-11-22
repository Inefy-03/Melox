package com.melox.player.data.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.system.Os
import com.melox.player.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.zip.ZipFile

internal data class UpdateAsset(val name: String, val url: String, val sha256: String?)
internal data class AppRelease(val tag: String, val notes: String, val asset: UpdateAsset?, val version: LocalDateTime?)

internal fun AppRelease.isNewerThan(installed: String): Boolean =
    isNewerBuild(version, tag, installed)

internal class AppUpdates(private val context: Context) {
    private val assetsByTag = mutableMapOf<String, UpdateAsset>()
    private val checkCache = context.getSharedPreferences("app_update_check_cache", Context.MODE_PRIVATE)

    fun claimAutomaticCheck(date: LocalDate): Boolean = synchronized(checkCache) {
        val key = date.toString()
        if (checkCache.getString("automatic_check_date", null) == key) {
            false
        } else {
            checkCache.edit().putString("automatic_check_date", key).apply()
            true
        }
    }

    fun cachedUpdateAvailable(date: LocalDate): Boolean =
        checkCache.getString("available_for", null) == availabilityKey(date)

    fun cacheAvailability(date: LocalDate, available: Boolean) {
        checkCache.edit().putString("available_for", if (available) availabilityKey(date) else null).apply()
    }

    private fun availabilityKey(date: LocalDate): String = "$date/${BuildConfig.VERSION_NAME}"

    suspend fun check(): AppRelease? = withContext(Dispatchers.IO) {
        JSONArray(request("$RELEASES?per_page=1"))
            .optJSONObject(0)?.takeUnless { it.optBoolean("draft") }?.toRelease()
    }

    suspend fun download(release: AppRelease, onProgress: suspend (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            val asset = assetsByTag[release.tag] ?: run {
                val tag = android.net.Uri.encode(release.tag)
                val updated = JSONObject(request("$RELEASES/tags/$tag")).toRelease()
                if (updated.tag != release.tag) throw IOException("Release tag changed")
                updated.asset ?: throw UpdateAssetUnavailable()
            }
            val directory = File(context.cacheDir, "app_updates")
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create update directory")
            val part = File.createTempFile("update-", ".part", directory)
            val apk = File(directory, part.nameWithoutExtension + ".apk")
            try {
                val connection = connect(asset.url)
                val digest = asset.sha256?.let { MessageDigest.getInstance("SHA-256") }
                try {
                    val length = connection.getHeaderField("Content-Length")?.toLongOrNull()
                    var received = 0L
                    onProgress(if (length != null && length > 0L) 0f else null)
                    connection.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var reportedPercent = 0
                            while (true) {
                                ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                digest?.update(buffer, 0, count)
                                received += count
                                if (length != null && length > 0L) {
                                    val percent = (received * 100 / length).toInt().coerceAtMost(100)
                                    if (percent != reportedPercent) {
                                        reportedPercent = percent
                                        onProgress(percent / 100f)
                                    }
                                }
                            }
                        }
                    }
                    if (length != null && received != length) throw IOException("Incomplete APK")
                } finally {
                    connection.disconnect()
                }
                if (digest != null && !digest.digest().joinToString("") { "%02x".format(it) }
                        .equals(asset.sha256, ignoreCase = true)
                ) throw InvalidUpdateApk()
                // Android's archive parser expects an APK path. Keep the unfinished file private.
                val validationFile = File(directory, part.nameWithoutExtension + "-validation.apk")
                try {
                    Os.link(part.absolutePath, validationFile.absolutePath)
                    if (!validateApk(validationFile, release.version ?: updateVersion(asset.name))) {
                        throw InvalidUpdateApk()
                    }
                } finally {
                    validationFile.delete()
                }
                if (!part.renameTo(apk)) throw IOException("Cannot finalize APK")
                apk
            } finally {
                part.delete()
            }
        }

    @Suppress("DEPRECATION")
    private fun validateApk(file: File, expectedVersion: LocalDateTime?): Boolean {
        val zipValid = runCatching {
            ZipFile(file).use { zip -> zip.getEntry("AndroidManifest.xml") != null }
        }.getOrDefault(false)
        if (!zipValid) return false
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = pm.getPackageInfo(context.packageName, flags)
        val signers = archive.currentSigners()
        if (signers.isEmpty() || signers != installed.currentSigners()) return false
        val name = archive.versionName ?: return false
        val version = updateVersion(name)
        return (expectedVersion == null || version == expectedVersion) &&
            isNewerBuild(version, name, installed.versionName ?: BuildConfig.VERSION_NAME)
    }

    private fun PackageInfo.currentSigners() =
        signingInfo?.apkContentsSigners?.toSet().orEmpty()

    private fun JSONObject.toRelease(): AppRelease {
        val tag = getString("tag_name")
        val firstApk = optJSONArray("assets")?.let { assets ->
            (0 until assets.length()).asSequence().mapNotNull(assets::optJSONObject)
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
        }
        val assetName = firstApk?.optString("name").orEmpty()
        val asset = firstApk?.takeIf { it.optString("state", "uploaded") == "uploaded" }
            ?.optString("browser_download_url")?.takeIf { it.isNotBlank() }?.let { url ->
            val digest = firstApk.optString("digest").substringAfter("sha256:", "")
                .takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
            UpdateAsset(assetName, url, digest)
        }
        if (asset == null) assetsByTag.remove(tag) else assetsByTag[tag] = asset
        return AppRelease(
            tag, optString("body", "").takeUnless { it == "null" }.orEmpty(), asset,
            updateVersion(assetName) ?: updateVersion(optString("name")) ?: updateVersion(tag),
        )
    }

    private fun request(url: String): String {
        val connection = connect(url)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun connect(url: String): HttpURLConnection {
        val endpoint = URL(url)
        if (endpoint.protocol != "https") throw IOException("HTTPS required")
        val connection = endpoint.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Accept", if (endpoint.host == "api.github.com") "application/vnd.github+json" else "application/octet-stream")
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            if (connection.url.protocol != "https") throw IOException("HTTPS required")
            connection
        } catch (error: Exception) {
            connection.disconnect()
            throw error
        }
    }
}

internal class UpdateAssetUnavailable : IOException()
internal class InvalidUpdateApk : IOException()

private const val RELEASES = "https://api.github.com/repos/Inefy-03/Melox/releases"
private val versionSuffix = Regex("-(\\d{10})$")
private val versionFormat = DateTimeFormatter.ofPattern("uuMMddHHmm", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
private val releaseTag = Regex("(?i)^v?(\\d+(?:\\.\\d+)*)(?:[-.]?(alpha|beta|rc)[.-]?(\\d*))?$")

internal fun updateVersion(name: String): LocalDateTime? {
    val suffix = versionSuffix.find(name.trim().replace(Regex("(?i)\\.apk$"), ""))?.groupValues?.get(1) ?: return null
    return runCatching { LocalDateTime.parse(suffix, versionFormat) }.getOrNull()
}

internal fun isNewerBuild(remoteTime: LocalDateTime?, remoteTag: String, installed: String): Boolean {
    val localTime = updateVersion(installed)
    if (remoteTime != null && localTime != null) return remoteTime > localTime

    fun parse(value: String) = releaseTag.matchEntire(
        value.trim().substringBefore('+').replace(versionSuffix, ""),
    )
    val remote = parse(remoteTag) ?: return false
    val local = parse(installed) ?: return false
    val remoteParts = remote.groupValues[1].split('.')
    val localParts = local.groupValues[1].split('.')
    for (index in 0 until maxOf(remoteParts.size, localParts.size)) {
        val comparison = (remoteParts.getOrNull(index)?.toLongOrNull() ?: 0L)
            .compareTo(localParts.getOrNull(index)?.toLongOrNull() ?: 0L)
        if (comparison != 0) return comparison > 0
    }
    val stages = listOf("alpha", "beta", "rc", "")
    val remoteStage = stages.indexOf(remote.groupValues[2].lowercase(Locale.ROOT))
    val localStage = stages.indexOf(local.groupValues[2].lowercase(Locale.ROOT))
    if (remoteStage != localStage) return remoteStage > localStage
    return (remote.groupValues[3].toLongOrNull() ?: 0L) > (local.groupValues[3].toLongOrNull() ?: 0L)
}
