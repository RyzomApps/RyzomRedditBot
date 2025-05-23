plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "de.InVinoVeritas"
version = findProperty("version") as? String ?: "1.0"

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

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "de.InVinoVeritas.RyzomRedditBot"
        )
    }
}

tasks.shadowJar {
    dependsOn(tasks.jar)
    archiveFileName.set("${rootProject.name}-${version}.jar")
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get())
            groupId = "de.InVinoVeritas"
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/RyzomApps/RyzomRedditBot")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}