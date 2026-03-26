package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class PikaIrGenerationExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val symbolFinder = createSymbolFinder(pluginContext)
    val poet = IRPoet(pluginContext, symbolFinder)
    val typeInfoCallTransformer = TypeInfoCallTransformer(pluginContext, poet)

    moduleFragment.transform(
      typeInfoCallTransformer,
      null
    )
  }
}
