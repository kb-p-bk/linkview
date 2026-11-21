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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LinkView"

// App module
include(":app")

// Core modules
include(":core:common")
include(":core:model")
include(":core:ui")

// Domain module
include(":domain")

// Data modules
include(":data:database")
include(":data:datastore")
include(":data:backup")
include(":data:repository")

// Feature modules
include(":feature:cards")
include(":feature:detail")
include(":feature:editor")
include(":feature:backup")
