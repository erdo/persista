buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.5.10")
        classpath("com.android.tools.build:gradle:4.2.2")
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
