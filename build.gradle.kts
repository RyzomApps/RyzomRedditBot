plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.invinoveritas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.jboss.org")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dean.jraw:JRAW:1.1.0")
    implementation("org.jsoup:jsoup:1.20.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    dependsOn(tasks.jar)
    archiveFileName.set("${rootProject.name}-${version}.jar")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "de.invinoveritas.RyzomRedditBot"
        )
    }
}

tasks.register<JavaExec>("debugJar") {
    dependsOn(tasks.shadowJar)
    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    //workingDir = jarFile.parentFile
    group = "application"
    description = "Run the JAR in debug mode"
    mainClass = "-jar"
    args = listOf(jarFile.absolutePath)
    jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
}