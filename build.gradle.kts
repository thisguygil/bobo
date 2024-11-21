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
    maven(url = "https://jitpack.io") // For Spotify Web API
    maven(url = "https://maven.lavalink.dev/releases") // For LavaPlayer and LavaLink YouTube
    maven(url = "https://maven.topi.wtf/releases") // For LavaSrc and LavaLyrics
}

dependencies {
    // Discord API
    implementation("net.dv8tion:JDA:5.2.1")

    // LavaPlayer
    implementation("dev.arbjerg:lavaplayer:2.2.2")
    implementation("dev.lavalink.youtube:v2:1.8.2")
    implementation("com.github.topi314.lavasrc:lavasrc:4.2.0")
    implementation("com.github.topi314.lavalyrics:lavalyrics:1.0.0")

    // JDA Utilities
    implementation("com.github.ygimenez:Pagination-Utils:4.1.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.google.apis:google-api-services-customsearch:v1-rev20240821-2.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20241105-2.0.0")
    implementation("com.google.guava:guava:33.3.1-jre")

    // Image Processing
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")

    // Database
    implementation("com.mysql:mysql-connector-j:9.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Other API Wrappers
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.1")
    implementation("io.github.sashirestela:simple-openai:3.9.2")

    // Other Java Utilities
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")
    implementation("commons-validator:commons-validator:1.9.0")
    implementation("org.json:json:20240303")
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