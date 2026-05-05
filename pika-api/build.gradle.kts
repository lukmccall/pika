plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.binary.compatibility.validator)
  alias(libs.plugins.vanniktech.mavenPublish)
}

version = libs.versions.pika.get()

kotlin {
  explicitApi()
  jvmToolchain(11)

  jvm()

  applyDefaultHierarchyTemplate()
}
