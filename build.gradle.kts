plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Required repository for DecentHolograms
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(libs.paper.api)
    // Use compileOnly as DecentHolograms is provided by the server environment
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.9")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
