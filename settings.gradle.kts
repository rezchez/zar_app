pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ZarfolioAndroid"
include(":app", ":core:common", ":core:model", ":core:security", ":core:network", ":core:designsystem", ":feature:authentication", ":feature:dashboard", ":feature:products", ":feature:camera")
