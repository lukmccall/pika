package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.intrinsic.PikaJvmIrIntrinsicSupport
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class PikaIrGenerationExtension(
  extraAnnotationFqNames: List<String>,
) : IrGenerationExtension {
  private val extraAnnotationClassIds: Set<ClassId> =
    extraAnnotationFqNames.mapTo(mutableSetOf()) { ClassId.topLevel(FqName(it)) }

  override fun generate(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext
  ) {
    val symbolFinder = createSymbolFinder(pluginContext)
    val poet = IRPoet(pluginContext, symbolFinder, moduleFragment, extraAnnotationClassIds)

    val introspectableTransformer = IntrospectableTransformer(
      pluginContext,
      poet,
      symbolFinder
    )

    val typeInfoCallTransformer = TypeInfoCallTransformer(poet)

    with(moduleFragment) {
      transform(introspectableTransformer, data = null)
      transform(typeInfoCallTransformer, data = null)
    }
  }

  override fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension? {
    val ctx = loweringContext as? JvmBackendContext ?: return null
    val irPluginContext = ctx.irPluginContext ?: return null
    return PikaJvmIrIntrinsicSupport(ctx, irPluginContext, extraAnnotationClassIds)
  }
}
