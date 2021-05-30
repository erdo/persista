plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val LIB_ARTIFACT_ID by extra("persista")
val LIB_DESCRIPTION by extra("persist single instances of kotlin data classes")

println("[$LIB_ARTIFACT_ID build file]")

dependencies {
    api("co.early.fore:fore-core-kt:1.4.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.30")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    testImplementation("junit:junit:4.12")
    testImplementation("io.mockk:mockk:1.10.5")
}

apply(from = "../publish-lib.gradle.kts")
