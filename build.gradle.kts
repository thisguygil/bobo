plugins {
    id("java")
    id("application")
}

group = "com.thisguygil"
version = "1.0"

application {
    mainClass.set("bobo.Bobo")
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

repositories {
    mavenCentral() // Most of the dependencies are in Maven Central
    maven(url = "https://maven.lavalink.dev/releases") // For LavaLink YouTube source
    maven(url = "https://maven.topi.wtf/releases") // For LavaSrc and LavaLyrics
}

dependencies {
    // Discord API and Utilities
    implementation("net.dv8tion:JDA:5.5.1")
    implementation("com.github.ygimenez:Pagination-Utils:4.1.2")

    // LavaPlayer
    implementation("dev.arbjerg:lavaplayer:2.2.3")
    implementation("dev.lavalink.youtube:v2:1.13.2")
    implementation("com.github.topi314.lavasrc:lavasrc:4.7.0")
    implementation("com.github.topi314.lavalyrics:lavalyrics:1.0.0")

    // Google APIs
    implementation("com.google.api-client:google-api-client:2.8.0")
    implementation("com.google.apis:google-api-services-customsearch:v1-rev20240821-2.0.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20250422-2.0.0")

    // Image Processing
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.12.0")

    // Database
    implementation("com.mysql:mysql-connector-j:9.3.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Local Testing
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    // Other API Wrappers
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.2.0")
    implementation("com.openai:openai-java:2.3.2")

    // Other Java Utilities
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    implementation("commons-validator:commons-validator:1.9.0")
    implementation("org.json:json:20250517")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    archiveBaseName.set("bobo")
    archiveVersion.set("1.0")
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("runtime-libs"))
}

tasks.named("build") {
    mustRunAfter(tasks.named("clean"))
    dependsOn(tasks.named("copyDependencies"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}