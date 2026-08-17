import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

val releaseKeystorePath = localProperties.getProperty("melox.keystore.path")
val releaseStorePassword = localProperties.getProperty("melox.store.password")
val releaseKeyPassword = localProperties.getProperty("melox.key.password")
val releaseKeyAlias = localProperties.getProperty("melox.key.alias")
val releaseKeystoreFile = releaseKeystorePath
    ?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }

val releaseSigningValues = listOf(
    "melox.keystore.path" to releaseKeystorePath,
    "melox.store.password" to releaseStorePassword,
    "melox.key.password" to releaseKeyPassword,
    "melox.key.alias" to releaseKeyAlias,
)
val releaseSigningConfigured = localPropertiesFile.isFile &&
    releaseKeystoreFile?.isFile == true &&
    releaseSigningValues.all { (_, value) -> !value.isNullOrBlank() }
val appVersionName = "1.0.0"
val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.equals("assemble", ignoreCase = true) ||
        taskName.endsWith(":assemble", ignoreCase = true) ||
        taskName.contains("release", ignoreCase = true)
}

if (releaseTaskRequested && !releaseSigningConfigured) {
    val missingValues = buildList {
        if (!localPropertiesFile.isFile) {
            add("local.properties")
        }
        if (!releaseKeystorePath.isNullOrBlank() && releaseKeystoreFile?.isFile != true) {
            add("keystore file: $releaseKeystorePath")
        }
        addAll(
            releaseSigningValues
                .filter { (_, value) -> value.isNullOrBlank() }
                .map { (name, _) -> name },
        )
    }.joinToString()
    throw GradleException(
        "Release signing is required for release builds. Missing: $missingValues. " +
            "Add melox.keystore.path, melox.store.password, melox.key.password, and melox.key.alias to local.properties.",
    )
}

android {
    namespace = "com.melox.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.melox.player"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = appVersionName

        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = requireNotNull(releaseKeystoreFile)
                storePassword = releaseStorePassword
                keyPassword = releaseKeyPassword
                keyAlias = releaseKeyAlias
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        localeFilters += setOf("en", "zh-rCN")
    }
}

@DisableCachingByDefault(because = "The output filename includes the execution-time clock.")
abstract class TimestampReleaseApkTask : DefaultTask() {
    @get:Internal
    abstract val apkDirectory: DirectoryProperty

    @get:Input
    abstract val versionName: Property<String>

    @TaskAction
    fun copyTimestampedApk() {
        val outputDirectory = apkDirectory.get().asFile
        val timestamp = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
        val targetFile = outputDirectory.resolve("Melox_${versionName.get()}_${timestamp}.apk")
        val sourceFile = outputDirectory
            .listFiles { candidate ->
                candidate.isFile &&
                    candidate.extension == "apk" &&
                    !candidate.name.startsWith("Melox_")
            }
            ?.maxByOrNull { candidate -> candidate.lastModified() }

        check(sourceFile?.isFile == true) {
            "Release APK was not generated in ${outputDirectory.absolutePath}"
        }

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setLastModified(System.currentTimeMillis())
        logger.lifecycle("Timestamped release APK: ${targetFile.absolutePath}")
    }
}

val timestampReleaseApk = tasks.register<TimestampReleaseApkTask>("timestampReleaseApk") {
    group = "build"
    description = "Copies the release APK to a timestamped filename."
    dependsOn("packageRelease")
    apkDirectory.set(layout.buildDirectory.dir("outputs/apk/release"))
    versionName.set(appVersionName)
}

tasks.matching { task -> task.name == "assembleRelease" }.configureEach {
    dependsOn(timestampReleaseApk)
}

dependencies {
    implementation(files("libs/renderscript-intrinsics-replacement-toolkit-344be3f-16k.aar"))
    implementation(files("libs/media3-decoder-ffmpeg-1.11.0-ffmpeg9.0-arm64-v8a.aar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.inspector)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.material.color.utilities)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.ui)
    implementation(libs.taglib)
    implementation(libs.tinypinyin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
