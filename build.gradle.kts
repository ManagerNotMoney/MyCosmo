plugins {
    id("java")
}

group = "io.github.managermoney"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {exclude("org.bukkit", "bukkit")}
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}