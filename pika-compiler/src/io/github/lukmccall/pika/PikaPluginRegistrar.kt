package io.github.lukmccall.pika

import io.github.lukmccall.pika.fir.IntrospectableDeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class PikaPluginRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::IntrospectableDeclarationGenerator
  }
}
