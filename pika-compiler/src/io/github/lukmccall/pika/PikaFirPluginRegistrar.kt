package io.github.lukmccall.pika

import io.github.lukmccall.pika.fir.FirIntrospectablePredicateMatcher
import io.github.lukmccall.pika.fir.IntrospectableDeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class PikaFirPluginRegistrar(
  private val extraAnnotationFqNames: List<String>,
) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::IntrospectableDeclarationGenerator
    +FirIntrospectablePredicateMatcher.getFactory(extraAnnotationFqNames)
  }
}
