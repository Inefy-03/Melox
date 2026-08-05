import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

abstract class PrintReleaseApkTask : DefaultTask() {
    @get:Internal
    abstract val apkDirectory: DirectoryProperty

    @TaskAction
    fun printOutput() {
        val file = apkDirectory.get().asFile
            .listFiles { candidate ->
                candidate.isFile &&
                    candidate.extension == "apk" &&
                    candidate.name.startsWith("Melox_")
            }
            ?.maxByOrNull { candidate -> candidate.lastModified() }
        check(file?.isFile == true) {
            "Release APK was not generated in ${apkDirectory.get().asFile.absolutePath}"
        }
        logger.lifecycle("Release APK: ${file.absolutePath}")
    }
}

project(":app").tasks.configureEach {
    if (name == "assembleRelease") {
        mustRunAfter(":app:clean")
        notCompatibleWithConfigurationCache(
            "Release APK filenames include the current build time.",
        )
    }
}

tasks.register<PrintReleaseApkTask>("assembleRelease") {
    group = "build"
    description = "Cleans, assembles, and prints the release APK output path."
    dependsOn(":app:clean", ":app:assembleRelease")
    apkDirectory.set(layout.projectDirectory.dir("app/build/outputs/apk/release"))
}
