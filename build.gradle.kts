plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "de.InVinoVeritas"
version = findProperty("version") as? String ?: "1.0.0"
val jarFileName = "${project.name}-${project.version}.jar"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.jboss.org")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-engine:1.12.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.12.2")

    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.jsoup:jsoup:1.20.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.shadowJar {
    dependsOn(tasks.build)
    archiveFileName.set(jarFileName)
    manifest {
        attributes(
            "Main-Class" to "de.InVinoVeritas.RyzomRedditBot"
        )
    }
}

tasks.register<JavaExec>("debugJar") {
    dependsOn(tasks.shadowJar)
    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    group = "application"
    description = "Run the JAR in debug mode"
    mainClass.set("-jar")
    args = listOf(jarFile.absolutePath)
    jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}