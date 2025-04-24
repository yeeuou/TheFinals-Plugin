plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.yeeuou"
version = "0.6-ALPHA"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    doLast {
        copy {
            from(archiveFile.get().asFile)
            into(".server/plugins")
        }
//        val arc = archiveFile.get().asFile
//        arc.copyTo(file(".server/plugins/${arc.name}"), true)
    }
}
