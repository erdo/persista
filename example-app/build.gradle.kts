plugins {
    id("com.android.application")
    id("maven")
    id("idea")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val appId = "foo.bar.example"

android {

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    compileSdkVersion(30)

    defaultConfig {
        applicationId = appId
        minSdkVersion(16)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            // keytool -genkey -v -keystore debug.fake_keystore -storetype PKCS12 -alias android -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 20000 -dname "cn=Unknown, ou=Unknown, o=Unknown, c=Unknown"
            storeFile = file("../keystore/debug.fake_keystore")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "../proguard-example-app.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    lintOptions {
        isAbortOnError = true
        lintConfig = File(project.rootDir, "lint-example-app.xml")
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {

    implementation(project(":persista-lib"))
    //implementation("co.early.persista:persista:0.2.1")

    implementation("co.early.fore:fore-kt-android:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.11.0")
}
