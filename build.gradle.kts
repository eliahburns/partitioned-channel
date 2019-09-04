import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

group = "io.github.eliahburns"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0")

    implementation("io.github.microutils:kotlin-logging:1.7.4")

    implementation("org.slf4j:slf4j-simple:1.7.26")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
    testCompile("junit", "junit", "4.12")
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<Test> {
        useJUnitPlatform { }
    }
}


