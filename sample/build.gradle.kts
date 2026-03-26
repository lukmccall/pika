plugins {
  alias(libs.plugins.kotlin.jvm)
  id("io.github.lukmccall.pika")
  application
}

application {
  mainClass.set("sample.MainKt")
}
