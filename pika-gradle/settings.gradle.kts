rootProject.name = "pika-gradle"

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.toml"))
      // Pin to the oldest supported Kotlin version so the compiled metadata
      // is readable by all consumers (e.g. expo-module-gradle-plugin on 2.1.20).
      version("kotlin", "2.1.20")
    }
  }
}

includeBuild("..") {
  dependencySubstitution {
    substitute(module("io.github.lukmccall.pika:pika-compiler"))
      .using(project(":pika-compiler"))
  }
}

includeBuild("../pika-api") {
  dependencySubstitution {
    substitute(module("io.github.lukmccall.pika:pika-api"))
      .using(project(":"))
  }
}
