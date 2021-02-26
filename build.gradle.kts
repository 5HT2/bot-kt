@file:Suppress("LocalVariableName", "PropertyName")

val bot_kt_version: String by project

group = "org.kamiblue"
version = bot_kt_version

plugins {
    kotlin("jvm") version "1.4.31"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    val kotlinx_coroutines_version: String by project
    val ktor_version: String by project
    val kordis_version: String by project
    val gson_version: String by project
    val nv_websocket_version: String by project
    val log4j_version: String by project
    val reflections_version: String by project

    implementation(kotlin("stdlib"))

    implementation("com.google.code.gson:gson:$gson_version")
    implementation("com.github.ronmamo:reflections:$reflections_version")
    implementation("com.github.Tea-Ayataka:Kordis:$kordis_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("org.apache.logging.log4j:log4j-api:$log4j_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")

    runtimeOnly("com.neovisionaries:nv-websocket-client:$nv_websocket_version")
    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4j_version")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j_version")
}

sourceSets.main {
    java {
        srcDir("src/main/cape-api")
        srcDir("src/main/command")
        srcDir("src/main/commons")
        srcDir("src/main/event")
        srcDir("src/main/github-discussion-api")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        manifest {
            attributes("Main-Class" to "org.kamiblue.botkt.Main")
        }
    }

    val sourceJar by register<Jar>("sourceJar") {
        group = "build"
        description = "Assemble API library source archive"

        archiveClassifier.set("sources")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets["main"].allSource)
    }

    artifacts {
        archives(jar)
        archives(sourceJar)
    }

    val binJar by register<Jar>("binJar") {
        group = "build"
        description = "Assemble binary archive with dependencies"

        archiveClassifier.set("bin")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets["main"].output)

        // copy deps to jar
        from(
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) it
                else zipTree(it)
            }
        )
    }

    register("buildAll") {
        group = "build"
        description = "Build all the jars"

        dependsOn(jar)
        dependsOn(sourceJar)
        dependsOn(binJar)
    }
}
