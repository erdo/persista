import co.early.persista.Shared

plugins {
    alias(libs.plugins.androidAppPlugin).apply(false)
    alias(libs.plugins.androidLibraryPlugin).apply(false)
    alias(libs.plugins.kotlinAndroidPlugin).apply(false)
    alias(libs.plugins.kotlinJvmPlugin).apply(false)
    alias(libs.plugins.kotlinMultiPlatformPlugin).apply(false)
    alias(libs.plugins.kotlinSerializationPlugin).apply(false)
    alias(libs.plugins.kotlinCocoapodsPlugin).apply(false)
    id("com.gradleup.nmcp.aggregation")
}

nmcpAggregation {
    centralPortal {
        username = Shared.Secrets.MAVEN_USER
        password = Shared.Secrets.MAVEN_PASSWORD
        publishingType = "USER_MANAGED" // USER_MANAGED | AUTOMATIC
    }
}

dependencies {
    nmcpAggregation(project(":persista"))
}