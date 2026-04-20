package io.github.lukmccall.pika

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PikaConfigurationKeys {
  val ENABLED: CompilerConfigurationKey<Boolean> =
    CompilerConfigurationKey.create("whether the Pika plugin is enabled")

  val INTROSPECTABLE_ANNOTATION: CompilerConfigurationKey<List<String>> =
    CompilerConfigurationKey.create("additional @Introspectable-equivalent annotation FqName")
}
