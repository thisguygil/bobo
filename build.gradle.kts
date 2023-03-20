plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.example"
version = "1.0"

val mainClassName = "bobo.Bobo"

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("net.dv8tion:JDA:5.0.0-beta.6")
    implementation("com.sedmelluq:lavaplayer:1.3.77")
    implementation("com.google.api-client:google-api-client:1.23.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.23.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation("io.github.cdimascio:dotenv-java:2.3.2")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.slf4j:slf4j-simple:2.0.6")
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