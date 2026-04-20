package io.github.lukmccall.pika

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class PikaCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String
    get() = BuildConfig.KOTLIN_PLUGIN_ID

  override val pluginOptions: Collection<CliOption> = listOf(ENABLED_OPTION, ANNOTATION_OPTION)

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    when (option) {
      ENABLED_OPTION -> configuration.put(PikaConfigurationKeys.ENABLED, value.toBoolean())
      ANNOTATION_OPTION -> configuration.appendList(PikaConfigurationKeys.INTROSPECTABLE_ANNOTATION, value)
      else -> error("Unexpected config option: '${option.optionName}'")
    }
  }

  companion object {
    val ENABLED_OPTION = CliOption(
      "enabled",
      "<true|false>",
      "Whether the Pika compiler plugin is enabled",
      required = false,
      allowMultipleOccurrences = false,
    )

    val ANNOTATION_OPTION = CliOption(
      "introspectableAnnotation",
      "<fqname>",
      "Fully qualified name of an annotation class to treat as @Introspectable",
      required = false,
      allowMultipleOccurrences = true,
    )
  }
}
