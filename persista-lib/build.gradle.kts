plugins {
    id("kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val LIB_ARTIFACT_ID by extra("persista")
val LIB_DESCRIPTION by extra("persist single instances of kotlin data classes")

println("[$LIB_ARTIFACT_ID build file]")

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation("co.early.fore:fore-kt-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.11.0")
}

apply(from = "../publish-lib.gradle.kts")
