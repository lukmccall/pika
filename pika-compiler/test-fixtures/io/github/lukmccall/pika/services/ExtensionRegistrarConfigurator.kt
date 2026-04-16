package io.github.lukmccall.pika.services

import io.github.lukmccall.pika.PikaConfigurationKeys
import io.github.lukmccall.pika.PikaPluginComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configurePlugin() {
  useConfigurators(::ExtensionRegistrarConfigurator)
  configureAnnotations()
}

/**
 * Fixtures can reference these FqNames as user-registered introspectable markers.
 * Matches the `-P plugin:<id>:introspectableAnnotation=<fqn>` CLI option at runtime.
 */
private val TEST_EXTRA_ANNOTATIONS = listOf("test.OptimizedRecord")

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
  private val registrar = PikaPluginComponentRegistrar()
  override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
    module: TestModule,
    configuration: CompilerConfiguration
  ) {
    configuration.put(PikaConfigurationKeys.INTROSPECTABLE_ANNOTATION, TEST_EXTRA_ANNOTATIONS)
    with(registrar) { registerExtensions(configuration) }
  }
}
