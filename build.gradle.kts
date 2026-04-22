import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
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
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

val rootJavaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
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

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(rootJavaLauncher)
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

tasks.register("runCatchTheFallingBall") {
    group = "application"
    description = "Runs the Scratch-style Catch The Falling Ball starter."
    dependsOn(":scratch-playground:run")
}

tasks.register("runCatchTheFallingBallSolution") {
    group = "application"
    description = "Runs the full Scratch-style Catch The Falling Ball solution."
    dependsOn(":scratch-playground:runCatchTheFallingBallSolution")
}
