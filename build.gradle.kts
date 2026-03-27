import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.10"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "codes.bed"
version = "0.1.0"
description = "Minestom mob spawning and AI library"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api("net.minestom:minestom:2026.03.25-1.21.11")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

kotlin {
    jvmToolchain(25)
}

mavenPublishing {
    coordinates(project.group.toString(), "mobs", project.version.toString())
    pom {
        name.set("mobs")
        description.set(project.description)
        inceptionYear.set("2026")
        url.set("https://github.com/bed-dev/stommobs")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("bed-dev")
                name.set("bed-dev")
                url.set("https://github.com/bed-dev")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/bed-dev/stommobs.git")
            developerConnection.set("scm:git:ssh://github.com/bed-dev/stommobs.git")
            url.set("https://github.com/bed-dev/stommobs")
        }
    }
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileTestKotlin") {
    compilerOptions {
        javaParameters.set(true)
    }
}

tasks.register<JavaExec>("runTestServer") {
    group = "application"
    description = "Runs the stommobs local test server for development"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("codes.bed.minestom.mobs.testing.LocalTestServerKt")
    standardInput = System.`in`
}
