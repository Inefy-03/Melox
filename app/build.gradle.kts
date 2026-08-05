import java.util.Properties
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
val releaseBuildDate = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    .format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
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
            ndk {
                abiFilters += setOf("armeabi-v7a", "arm64-v8a")
            }
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

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("Melox_${appVersionName}_${releaseBuildDate}.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.media3.exoplayer)
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
