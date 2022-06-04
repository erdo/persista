plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

val appId = "foo.bar.example"

android {

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    compileSdk = 31

    defaultConfig {
        applicationId = appId
        minSdk = 16
        targetSdk = 31
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
    lint {
        abortOnError = true
        lintConfig = File(project.rootDir, "lint-example-app.xml")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {

    implementation(project(":persista-lib"))
    //implementation("co.early.persista:persista:1.0.0")

    implementation("co.early.fore:fore-kt-android:1.5.13")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.11.0")
}
