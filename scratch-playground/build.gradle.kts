import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm") version "2.2.21"
    application
}

val macOsJdkPackages = listOf(
    "java.desktop/sun.java2d.opengl",
    "java.desktop/java.awt",
    "java.desktop/sun.awt",
    "java.desktop/sun.lwawt",
    "java.desktop/sun.lwawt.macosx",
    "java.desktop/com.apple.eawt",
    "java.desktop/com.apple.eawt.event",
)

val macOsJvmModuleArgs = buildList {
    for (packageName in macOsJdkPackages) {
        add("--add-opens=$packageName=ALL-UNNAMED")
        add("--add-exports=$packageName=ALL-UNNAMED")
    }
}

val scratchPlaygroundJvmArgs = if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
    macOsJvmModuleArgs
} else {
    emptyList()
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(project(":"))
    implementation("com.soywiz.korge:korge-jvm:5.2.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("de.moritzf.picoboard.scratch.examples.catchthefallingball.MainKt")
    applicationDefaultJvmArgs = scratchPlaygroundJvmArgs
}

tasks.named<JavaExec>("run") {
    jvmArgs(scratchPlaygroundJvmArgs)
}
