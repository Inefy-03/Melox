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

tasks.register<PrintReleaseApkTask>("assembleRelease") {
    group = "build"
    description = "Assembles the release APK and prints its output path."
    dependsOn(":app:assembleRelease")
    apkDirectory.set(layout.projectDirectory.dir("app/build/outputs/apk/release"))
}
