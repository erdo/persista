buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
    }
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}
