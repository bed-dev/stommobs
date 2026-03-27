import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    `java-library`
    `maven-publish`
}

group = "codes.bed"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api("net.minestom:minestom:2026.03.25-1.21.11")
    implementation(kotlin("stdlib"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}


tasks.register<JavaExec>("runTestServer") {
    group = "application"
    description = "Runs the stommobs local test server for development"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("codes.bed.minestom.mobs.testing.LocalTestServerKt")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "stommobs"

            pom {
                name.set("stommobs")
                description.set("Minestom mob spawning and AI library")
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
                    }
                }
                scm {
                    url.set("https://github.com/bed-dev/stommobs")
                    connection.set("scm:git:git://github.com/bed-dev/stommobs.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bed-dev/stommobs.git")
                }
            }
        }
    }
}

kotlin {
    jvmToolchain(25)
}

