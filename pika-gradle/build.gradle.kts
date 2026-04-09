import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.buildconfig)
  alias(libs.plugins.gradle.plugin)
  id("com.vanniktech.maven.publish") version "0.30.0"
}

kotlin {
  jvmToolchain(11)
}

// Coordinates for the compiler plugin artifacts (must match root build.gradle.kts)
val pluginGroup = "io.github.lukmccall.pika"
val pikaVersion: String = libs.versions.pika.get()
val kotlinVersion: String = libs.versions.kotlin.asProvider().get()
val pluginVersion = "$pikaVersion-$kotlinVersion"

group = pluginGroup
version = pluginVersion

sourceSets {
  main {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
  }
  test {
    java.setSrcDirs(listOf("test"))
    resources.setSrcDirs(listOf("testResources"))
  }
}

dependencies {
  implementation(libs.kotlin.gradle.plugin.api)
  testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
  packageName(pluginGroup)

  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"$pluginGroup\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"$pluginGroup\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"pika-compiler\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"$pluginVersion\"")
  buildConfigField(
    type = "String",
    name = "ANNOTATIONS_LIBRARY_COORDINATES",
    expression = "\"$pluginGroup:pika-api:$pluginVersion\""
  )
}

gradlePlugin {
  plugins {
    create("PikaPlugin") {
      id = pluginGroup
      displayName = "PikaPlugin"
      description = "PikaPlugin"
      implementationClass = "io.github.lukmccall.pika.PikaGradlePlugin"
    }
  }
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

  // Only sign when signing credentials are available (CI environment)
  if (project.findProperty("signingInMemoryKey") != null) {
    signAllPublications()
  }

  configure(GradlePlugin(JavadocJar.Empty(), sourcesJar = true))

  pom {
    name = "Pika Gradle Plugin"
    description = "Gradle plugin for Pika - Kotlin compiler plugin for generating type information in the compile time"
    inceptionYear = "2025"
    url = "https://github.com/lukmccall/pika"
    licenses {
      license {
        name = "The MIT License"
        url = "https://opensource.org/license/mit"
        distribution = "https://opensource.org/license/mit"
      }
    }
    developers {
      developer {
        id = "lukmccall"
        name = "Łukasz Kosmaty"
        url = "https://github.com/lukmccall"
      }
    }
    scm {
      url = "https://github.com/lukmccall/pika"
      connection = "scm:git:git://github.com/lukmccall/pika.git"
      developerConnection = "scm:git:ssh://github.com/lukmccall/pika.git"
    }
  }
}
