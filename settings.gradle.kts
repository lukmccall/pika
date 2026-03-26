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
include("pika-api")
include("sample")
