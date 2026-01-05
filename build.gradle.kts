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


