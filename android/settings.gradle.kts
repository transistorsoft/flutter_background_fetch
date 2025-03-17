import kotlin.io.path.name

        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
                maven {
                    url = uri("./libs")
                }
            }
        }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("./libs")
        }
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

rootProject.name = "background_fetch"
include(":app")