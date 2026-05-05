package io.github.lukmccall.pika

import io.github.lukmccall.pika.fir.FirIntrospectablePredicateMatcher
import io.github.lukmccall.pika.fir.IntrospectableDeclarationGenerator
import io.github.lukmccall.pika.fir.IntrospectableSupertypeGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class PikaFirPluginRegistrar(
  private val extraAnnotationFqNames: List<String>,
) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::IntrospectableDeclarationGenerator
    +::IntrospectableSupertypeGenerator
    +FirIntrospectablePredicateMatcher.getFactory(extraAnnotationFqNames)
  }
}
