plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.binary.compatibility.validator)
  alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
  explicitApi()
  jvmToolchain(11)

  jvm()

  applyDefaultHierarchyTemplate()
}
