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
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    implementation("net.dv8tion:JDA:5.0.0-beta.14")
    implementation("com.github.ygimenez:Pagination-Utils:4.0.6")
    implementation("com.theokanning.openai-gpt3-java:service:0.16.0")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20230904-2.0.0")
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