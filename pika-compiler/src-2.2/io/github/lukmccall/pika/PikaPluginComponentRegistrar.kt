package io.github.lukmccall.pika

import io.github.lukmccall.pika.ir.PikaIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class PikaPluginComponentRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val extraAnnotations = configuration.get(PikaConfigurationKeys.INTROSPECTABLE_ANNOTATION).orEmpty()
    FirExtensionRegistrarAdapter.registerExtension(PikaFirPluginRegistrar(extraAnnotations))
    IrGenerationExtension.registerExtension(PikaIrGenerationExtension(extraAnnotations))
  }
}
