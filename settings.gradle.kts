include(":example-app")
include(":persista-lib")
rootProject.name = "persista"

// https://youtrack.jetbrains.com/issue/KTIJ-24981
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

