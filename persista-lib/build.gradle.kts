plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val LIB_ARTIFACT_ID by extra("persista")
val LIB_DESCRIPTION by extra("persist single instances of kotlin data classes")

println("[$LIB_ARTIFACT_ID build file]")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api("co.early.fore:fore-kt-core:1.4.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.11.0")
}

apply(from = "../publish-lib.gradle.kts")
