pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Fallback when dl.google.com is unreachable.
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // dl.google.com is blackholed on some networks. Ask reachable repos
        // first, then Google Maven for anything the mirror lacks.
        mavenCentral()
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
    }
}

rootProject.name = "CursorMobile"
include(":app")
