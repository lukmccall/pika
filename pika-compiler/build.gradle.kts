plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.buildconfig)
  alias(libs.plugins.gradle.java.test.fixtures)
  alias(libs.plugins.gradle.idea)
  alias(libs.plugins.vanniktech.mavenPublish)
}

val kotlinVersionStr: String = libs.versions.kotlin.asProvider().get()
val kotlinMinorVersion = kotlinVersionStr.split(".").take(2).joinToString(".")

val mainSourceDir = when {
  kotlinVersionStr >= "2.3.20" -> "src-2.3.20+"
  kotlinVersionStr >= "2.3.0" -> "src-2.3"
  else -> "src-2.1"  // 2.1.x and 2.2.x have the same API
}

val testFixturesSourceDir = when {
  kotlinMinorVersion >= "2.3" -> "test-fixtures-2.3+"
  kotlinMinorVersion >= "2.2" -> "test-fixtures-2.2"
  else -> "test-fixtures-2.1"
}

val testDataDir = when {
  kotlinMinorVersion >= "2.3" -> "testData"
  kotlinMinorVersion >= "2.2" -> "testData-2.2"
  else -> "testData-2.1"
}

sourceSets {
  main {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
    kotlin.srcDir(mainSourceDir)
  }
  testFixtures {
    java.setSrcDirs(listOf("test-fixtures"))
    kotlin.srcDir(testFixturesSourceDir)
  }
  test {
    java.setSrcDirs(listOf("test", "test-gen"))
    resources.setSrcDirs(listOf(testDataDir))
  }
}

idea {
  module.generatedSourceDirs.add(projectDir.resolve("test-gen"))
}

val annotationsRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }
val testArtifacts: Configuration by configurations.creating

dependencies {
  compileOnly(libs.kotlin.compiler)

  testFixturesApi(libs.kotlin.test.junit5)
  testFixturesApi(libs.kotlin.test.framework)
  testFixturesApi(libs.kotlin.compiler)
  testFixturesRuntimeOnly(libs.junit)

  annotationsRuntimeClasspath(project(":pika-api"))

  // Dependencies required to run the internal test framework.
  testArtifacts(libs.kotlin.stdlib)
  testArtifacts(libs.kotlin.stdlib.jdk8)
  testArtifacts(libs.kotlin.reflect)
  testArtifacts(libs.kotlin.test)
  testArtifacts(libs.kotlin.script.runtime)
  testArtifacts(libs.kotlin.annotations.jvm)
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
  }

  packageName(group.toString())
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")
}

tasks.test {
  dependsOn(annotationsRuntimeClasspath)

  useJUnitPlatform()
  workingDir = rootDir

  systemProperty("annotationsRuntime.classpath", annotationsRuntimeClasspath.asPath)

  // Properties required to run the internal test framework.
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

  systemProperty("idea.ignore.disabled.plugins", "true")
  systemProperty("idea.home.path", rootDir)
}

kotlin {
  jvmToolchain(11)
  compilerOptions {
    optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
  }
}

val generateTests by tasks.registering(JavaExec::class) {
  inputs.dir(layout.projectDirectory.dir("testData"))
    .withPropertyName("testData")
    .withPathSensitivity(PathSensitivity.RELATIVE)
  outputs.dir(layout.projectDirectory.dir("test-gen"))
    .withPropertyName("generatedTests")

  classpath = sourceSets.testFixtures.get().runtimeClasspath
  mainClass.set("io.github.lukmccall.pika.GenerateTestsKt")
  workingDir = rootDir
}

tasks.compileTestKotlin {
  dependsOn(generateTests)
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
  val path = testArtifacts.files
    .find { """$jarName-\d.*""".toRegex().matches(it.name) }
    ?.absolutePath
    ?: return
  systemProperty(propName, path)
}
