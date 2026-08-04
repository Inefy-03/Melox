import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

abstract class PrintReleaseApkTask : DefaultTask() {
    @get:InputFile
    abstract val apkFile: RegularFileProperty

    @TaskAction
    fun printOutput() {
        val file = apkFile.get().asFile
        check(file.isFile) {
            "Release APK was not generated at ${file.absolutePath}"
        }
        logger.lifecycle("Release APK: ${file.absolutePath}")
    }
}

tasks.register<PrintReleaseApkTask>("assembleRelease") {
    group = "build"
    description = "Assembles the release APK and prints its output path."
    dependsOn(":app:assembleRelease")
    apkFile.set(layout.projectDirectory.file("app/build/outputs/apk/release/app-release.apk"))
}
