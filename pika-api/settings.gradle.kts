rootProject.name = "pika-api"

pluginManagement {
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
      from(files("../gradle/libs.toml"))
      // Pin to the oldest supported Kotlin version so the compiled metadata
      // is readable by all supported Kotlin compilers (2.1.20+).
      version("kotlin", "2.1.20")
    }
  }
}
