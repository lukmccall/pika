package io.github.lukmccall.pika

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PikaConfigurationKeys {
  val INTROSPECTABLE_ANNOTATION: CompilerConfigurationKey<List<String>> =
    CompilerConfigurationKey.create("additional @Introspectable-equivalent annotation FqName")
}
