buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.4.30")
        classpath("com.android.tools.build:gradle:4.1.2")
    }
}

plugins {
    id("idea")
}

allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}
