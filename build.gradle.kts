import com.needhamsoftware.unojar.gradle.PackageUnoJarTask

plugins {
    id("java-library")
    id("com.needhamsoftware.unojar") version "1.1.0"
}

group = "com.skadistats"
version = "3.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.skadistats:clarity:3.0.6")
    api("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.google.code.gson:gson:2.8.9")
}

File("src/main/java/skadistats/clarity/examples").walk().maxDepth(1).forEach {
    tasks.register<JavaExec>("${it.name}Run") {
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
    }
    tasks.register<PackageUnoJarTask>("${it.name}Package") {
        dependsOn("jar")
        archiveBaseName.set(it.name)
        archiveVersion.set("")
        archiveClassifier.set("")
        mainClass.set("skadistats.clarity.examples.${it.name}.Main")
    }
}
