pluginManagement {
  includeBuild("pika-gradle")
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
  versionCatalogs {
    create("libs") {
      from(files("gradle/libs.toml"))
      providers.gradleProperty("kotlinVersion").orNull?.let { kotlinVersion ->
        version("kotlin", kotlinVersion)
      }
    }
  }
}

rootProject.name = "pika"

include("pika-compiler")
include("sample")

includeBuild("pika-api") {
  dependencySubstitution {
    substitute(module("io.github.lukmccall.pika:pika-api")).using(project(":"))
  }
}
