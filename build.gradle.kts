import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
  alias(libs.plugins.buildconfig) apply false
  alias(libs.plugins.vanniktech.mavenPublish) apply false
}

val pikaVersion: String = libs.versions.pika.get()
val kotlinVersion: String = libs.versions.kotlin.asProvider().get()

allprojects {
  group = "io.github.lukmccall.pika"
  version = "$pikaVersion-$kotlinVersion"
}

subprojects {
  plugins.withId("com.vanniktech.maven.publish") {
    extensions.configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

      // Only sign when signing credentials are available (CI environment)
      if (project.findProperty("signingInMemoryKey") != null) {
        signAllPublications()
      }

      pom {
        name = project.name
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
  }
}
