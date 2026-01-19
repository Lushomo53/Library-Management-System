plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "com.library"
version = "1.0-SNAPSHOT"

val javafxVersion = "24"

repositories {
    mavenCentral()
}

dependencies {
    // JavaFX dependencies
    implementation ("org.openjfx:javafx-controls:17.0.0")
    implementation ("org.openjfx:javafx-fxml:17.0.0")
    implementation ("org.openjfx:javafx-graphics:17.0.0")

    // MySQL
    implementation("com.mysql:mysql-connector-j:9.3.0")

    //by-crypt hashing
    implementation("org.mindrot:jbcrypt:0.4")

    //google auth api
    implementation("com.google.api-client:google-api-client:2.4.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.35.0")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20240520-2.0.0")

    //email services
    implementation("com.sun.mail:javax.mail:1.6.2")

    //pdf export tools
    implementation("com.github.librepdf:openpdf:1.3.32")

    // Testing dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.library.Main")
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}


