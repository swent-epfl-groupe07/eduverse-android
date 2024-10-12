// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.ktfmt) apply false
    alias(libs.plugins.gms) apply false

    // Apply the SonarQube plugin
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.projectKey", "swent-epfl-groupe07_eduverse-android")
        property("sonar.organization", "swent-epfl-groupe07")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
