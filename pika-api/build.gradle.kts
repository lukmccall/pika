import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.binary.compatibility.validator)
  id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.lukmccall.pika"
version = libs.versions.pika.get()

kotlin {
  explicitApi()
  jvmToolchain(11)
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

  if (project.findProperty("signingInMemoryKey") != null) {
    signAllPublications()
  }

  configure(KotlinJvm(JavadocJar.Empty(), sourcesJar = true))

  pom {
    name = "pika-api"
    description = "Pika - Kotlin compiler plugin for generating type information in the compile time"
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
