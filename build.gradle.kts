import org.gradle.api.tasks.options.Option

plugins {
    id("java")
    alias(libs.plugins.loom)
}

group = "me.garfxld"
version = "1.0-SNAPSHOT"

loom {
    serverOnlyMinecraftJar()
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
}


/** Adds `--output`, the required destination for the generated assets. */
abstract class DataGenTask : JavaExec() {

    @get:Input
    @get:Optional
    @get:Option(option = "output", description = "Directory the generated assets are written to.")
    abstract val output: Property<String>

    override fun exec() {
        args("--output", output.orNull ?: error("`datagen` requires `--output=<dir>`."))
        super.exec()
    }
}

tasks.register<DataGenTask>("datagen") {
    group = "build"
    description = "Runs the data generator."
    mainClass.set("me.garfxld.DataGen")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = projectDir
}
