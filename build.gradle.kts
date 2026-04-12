import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    `java-library`
    application
    `maven-publish`
}

group = "de.moritzf.picoboard"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val starterExample by sourceSets.creating {
    java.srcDir("examples/first-project-kotlin/src/main/kotlin")
    resources.srcDir("examples/first-project-kotlin/src/main/resources")

    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().output
}

configurations[starterExample.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[starterExample.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    explicitApi()
    jvmToolchain(11)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation("com.fazecast:jSerialComm:2.11.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(tasks.named(starterExample.classesTaskName))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Automatic-Module-Name"] = "de.moritzf.picoboard"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("picoboard")
                description.set("Kotlin/JVM library for using a PicoBoard from Kotlin and Java.")
            }
        }
    }
}

application {
    mainClass.set("de.moritzf.picoboard.cli.PicoBoardCliKt")
}

tasks.register<JavaExec>("runFirstProjectKotlin") {
    group = "application"
    description = "Runs the beginner Kotlin starter example."
    classpath = starterExample.runtimeClasspath
    mainClass.set("de.moritzf.picoboard.examples.firstproject.MainKt")
}
