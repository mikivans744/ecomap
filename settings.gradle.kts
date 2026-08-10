import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://api.mapbox.com/downloads/v2/releases/maven") {
            credentials {
                username = "mapbox"
                val secrets = Properties().apply {
                    val secretsFile = file("secrets.properties")
                    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
                }
                password = secrets.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")
            }
            authentication { create<BasicAuthentication>("basic") }
        }
    }
}

rootProject.name = "ECO-MAP"
include(":app")
