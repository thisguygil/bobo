plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.thisguygil"
version = "1.0"

val mainClassName = "bobo.Bobo"

repositories {
    mavenCentral() // Most of the dependencies are in Maven Central
    maven(url = "https://jitpack.io") // For Spotify Web API
    maven(url = "https://maven.lavalink.dev/releases") // For LavaPlayer and LavaLink YouTube
    maven(url = "https://maven.topi.wtf/releases") // For LavaSrc and LavaLyrics
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("net.dv8tion:JDA:5.0.2")
    implementation("com.github.ygimenez:Pagination-Utils:4.0.6")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("dev.arbjerg:lavaplayer:2.2.1")
    implementation("dev.lavalink.youtube:v2:1.6.0")
    implementation("com.github.topi314.lavasrc:lavasrc:4.2.0")
    implementation("com.github.topi314.lavalyrics:lavalyrics:1.0.0")
    implementation("com.google.api-client:google-api-client:2.6.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240514-2.0.0")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.1")
    implementation("net.coobird:thumbnailator:0.4.20")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.11.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.json:json:20240303")
    implementation("com.google.guava:guava:33.3.0-jre")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("commons-validator:commons-validator:1.9.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.github.cdimascio:dotenv-java:3.0.1")
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