plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Required repository for DecentHolograms
    maven { url = uri("https://jitpack.io") }
    // Required repository for PlotSquared
    maven("https://repo.papermc.io/repository/maven-public/")
    // Required repository for FastAsyncWorldEdit
    maven { url = uri("https://mvn.enginehub.org/repo") }
}

dependencies {
    compileOnly(libs.paper.api)
    // Use compileOnly as DecentHolograms is provided by the server environment
    compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.9")
    // PlotSquared API - provided by the server environment
    implementation(platform("com.intellectualsites.bom:bom-newest:1.56"))
    compileOnly("com.intellectualsites.plotsquared:plotsquared-core")
    compileOnly("com.intellectualsites.plotsquared:plotsquared-bukkit") { isTransitive = false }
    // FastAsyncWorldEdit (https://intellectualsites.gitbook.io/fastasyncworldedit/api/api-usage)
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    // to use bukkit adapter
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
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
