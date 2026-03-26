package io.github.lukmccall.pika

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class PikaPluginRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() = Unit
}
