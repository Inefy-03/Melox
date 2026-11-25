pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("third_party/media3-exoplayer/maven")
            content {
                includeModule("androidx.media3", "media3-exoplayer")
            }
        }
        maven {
            url = uri("third_party/compose-ui/maven")
            content {
                includeModule("androidx.compose.ui", "ui-android")
            }
        }
        exclusiveContent {
            forRepository { maven { url = uri("third_party/miuix/maven") } }
            filter {
                includeVersionByRegex(
                    "top\\.yukonga\\.miuix\\.kmp",
                    "miuix-(ui|glass|material|nav|preference|squircle)-android",
                    "0\\.9\\.5-beta04\\.1-melox-[a-f0-9]+",
                )
            }
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroup("com.github.promeg")
            }
        }
    }
}

rootProject.name = "Melox"
include(":app")
