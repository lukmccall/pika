rootProject.name = "pika-gradle"

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.toml"))
      providers.gradleProperty("kotlinVersion").orNull?.let { kotlinVersion ->
        version("kotlin", kotlinVersion)
      }
    }
  }
}

includeBuild("..") {
  dependencySubstitution {
    substitute(module("io.github.lukmccall.pika:pika-compiler"))
      .using(project(":pika-compiler"))
    substitute(module("io.github.lukmccall.pika:pika-api"))
      .using(project(":pika-api"))
  }
}
