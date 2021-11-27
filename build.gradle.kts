val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val kmongoVersion: String by project
val koinVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.kiptechie"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    project.setProperty("mainClassName", mainClass.get())
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val sshAntTask = configurations.create("sshAntTask")

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

    // KMongo
    implementation("org.litote.kmongo:kmongo:$kmongoVersion")
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongoVersion")

    // Koin core features
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    sshAntTask("org.apache.ant:ant-jsch:1.10.11")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
}

ant.withGroovyBuilder {
    "taskdef"(
        "name" to "scp",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
        "classpath" to configurations["sshAntTask"].asPath
    )
    "taskdef"(
        "name" to "ssh",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
        "classpath" to configurations["sshAntTask"].asPath
    )
}


task("deploy") {
    dependsOn("clean", "shadowJar")
    ant.withGroovyBuilder {
        doLast {
            val knownHosts = File.createTempFile("knownhosts", "txt")
            val user = "poet"
            val host = "172.105.123.90"
            val pk = file("keys/ktorchat")
            val jarFileName = "com.kiptechie.ktor-chat-$version-all.jar"
            try {
                "scp"(
                    "file" to file("build/libs/$jarFileName"),
                    "todir" to "$user@$host:~/ktor-chat",
                    "keyfile" to pk,
                    "trust" to true,
                    "knownhosts" to knownHosts
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to pk,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "mv ~/ktor-chat/$jarFileName ~/ktor-chat/chat-server.jar"
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to pk,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "sudo systemctl stop ktor-chat"
                )
                "ssh"(
                    "host" to host,
                    "username" to user,
                    "keyfile" to pk,
                    "trust" to true,
                    "knownhosts" to knownHosts,
                    "command" to "sudo systemctl start ktor-chat"
                )
            } finally {
                knownHosts.delete()
            }
        }
    }
}