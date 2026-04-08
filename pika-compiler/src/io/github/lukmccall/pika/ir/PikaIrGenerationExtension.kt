package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.intrinsic.PikaJvmIrIntrinsicSupport
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class PikaIrGenerationExtension : IrGenerationExtension {
  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext
  ) {
    val symbolFinder = createSymbolFinder(pluginContext)
    val poet = IRPoet(pluginContext, symbolFinder)

    // Generate __PIntrospectionData() for Introspectable classes
    val introspectableTransformer = IntrospectableTransformer(
      pluginContext,
      poet,
      symbolFinder
    )

    // Transform typeInfo calls (including pIntrospectionOf)
    val typeInfoCallTransformer = TypeInfoCallTransformer(poet)

    with(moduleFragment) {
      transform(introspectableTransformer, data = null)
      transform(typeInfoCallTransformer, data = null)
    }
  }

  override fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension? {
    val ctx = loweringContext as? JvmBackendContext ?: return null
    val irPluginContext = ctx.irPluginContext ?: return null
    return PikaJvmIrIntrinsicSupport(ctx, irPluginContext)
  }
}
