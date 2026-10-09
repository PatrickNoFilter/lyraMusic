@file:Suppress("UnstableApiUsage")


pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "Lyra Music"
include(":app")
include(":innertube")

// Automatically initialize git submodules if missing (e.g. CI / partial checkouts)
if (!file("lyrics/kugou/build.gradle.kts").exists() && file(".git").exists()) {
    try {
        println("Submodules missing; initializing via git...")
        ProcessBuilder("git", "submodule", "update", "--init", "--recursive")
            .directory(rootDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(120, java.util.concurrent.TimeUnit.SECONDS)
    } catch (e: Exception) {
        println("Could not initialize submodules: ${e.message}")
    }
}

fun includeIfPresent(path: String, directory: String) {
    if (file(directory).isDirectory) include(path)
}

includeIfPresent(":lyrics:kugou", "lyrics/kugou")
includeIfPresent(":lyrics:lrclib", "lyrics/lrclib")
includeIfPresent(":lyrics:simpmusic", "lyrics/simpmusic")
includeIfPresent(":lyrics:betterlyrics", "lyrics/betterlyrics")
includeIfPresent(":lyrics:paxsenix", "lyrics/paxsenix")
includeIfPresent(":lyrics:unison", "lyrics/unison")
includeIfPresent(":lyrics:youlyplus", "lyrics/youlyplus")
include(":lastfm")
include(":kizzy")
include(":canvas")
include(":shazamkit")

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that OpenTune and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in innertube/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.newpipe.extractor)
// To:
//      implementation("com.github.teamnewpipe:NewPipeExtractor")
//includeBuild("../NewPipeExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.teamnewpipe:NewPipeExtractor")).using(project(":extractor"))
//    }
//}
