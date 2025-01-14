plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.2"
}

group = "com.thisguygil"
version = "1.0"

val mainClassName = "bobo.Bobo"

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

repositories {
    mavenCentral() // Most of the dependencies are in Maven Central
    maven(url = "https://maven.lavalink.dev/releases") // For LavaLink YouTube source
    maven(url = "https://maven.topi.wtf/releases") // For LavaSrc and LavaLyrics
}

dependencies {
    // Discord API
    implementation("net.dv8tion:JDA:5.2.2")

    // LavaPlayer
    implementation("dev.arbjerg:lavaplayer:2.2.2")
    implementation("dev.lavalink.youtube:v2:1.11.3")
    implementation("com.github.topi314.lavasrc:lavasrc:4.3.0")
    implementation("com.github.topi314.lavalyrics:lavalyrics:1.0.0")

    // JDA Utilities
    implementation("com.github.ygimenez:Pagination-Utils:4.1.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.7.1")
    implementation("com.google.apis:google-api-services-customsearch:v1-rev20240821-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20250101-2.0.0")

    // Image Processing
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")

    // Database
    implementation("com.mysql:mysql-connector-j:9.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Local Testing
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.0")

    // Other API Wrappers
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.1.1")
    implementation("com.openai:openai-java:0.11.2")

    // Other Java Utilities
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")
    implementation("commons-validator:commons-validator:1.9.0")
    implementation("org.json:json:20250107")
}

tasks.jar {
    manifest {
        attributes(
                "Main-Class" to mainClassName
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("run") {
    classpath = project.configurations.getByName("runtimeClasspath")
    mainClass.set("bobo.Bobo")
}

tasks.named("build") {
    mustRunAfter(tasks.named("clean"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}