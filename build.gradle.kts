plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.example"
version = "1.0"

val mainClassName = "bobo.Bobo"

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
    maven(url = "https://maven.lavalink.dev/releases")
    maven(url = "https://maven.topi.wtf/releases")
}

dependencies {
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("net.dv8tion:JDA:5.0.0-beta.24")
    implementation("com.github.ygimenez:Pagination-Utils:4.0.6")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("dev.arbjerg:lavaplayer:2.2.0")
    implementation("dev.lavalink.youtube:v2:1.3.0")
    implementation("com.github.topi314.lavasrc:lavasrc:4.1.1")
    implementation("com.github.topi314.lavalyrics:lavalyrics:1.0.0")
    implementation("com.google.api-client:google-api-client:2.3.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.35.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240213-2.0.0")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:8.4.0")
    implementation("net.coobird:thumbnailator:0.4.20")
    runtimeOnly("com.twelvemonkeys.imageio:imageio-webp:3.10.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.json:json:20240205")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("commons-validator:commons-validator:1.7")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
}

tasks.jar {
    manifest {
        attributes(
                "Main-Class" to mainClassName
        )
    }
}

tasks.register("stage") {
    dependsOn("build", "clean")
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