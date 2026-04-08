package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.symbols.PikaAPI
import io.github.lukmccall.pika.symbols.KotlinStd
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

@Suppress("ClassName")
class SymbolFinder(
  private val classResolver: (ClassId) -> IrClassSymbol?,
  private val functionResolver: (CallableId) -> Collection<IrSimpleFunctionSymbol>
) {
  val pikaAPI = _PikaAPI()

  inner class _PikaAPI {
    val pType by cachedReference(PikaAPI.PType)
    val pTypeDescriptor = _PTypeDescriptor()

    inner class _PTypeDescriptor {
      val root by cachedReference(PikaAPI.PTypeDescriptor.Root)

      val concrete by cachedReference(PikaAPI.PTypeDescriptor.Concrete)
      val parameterized by cachedReference(PikaAPI.PTypeDescriptor.Parameterized)
      val star by cachedReference(PikaAPI.PTypeDescriptor.Star)
    }

    // Introspectable types
    val introspectable by cachedReference(PikaAPI.Introspectable)
    val pVisibility by cachedReference(PikaAPI.PVisibility)
    val pProperty by cachedReference(PikaAPI.PProperty)
    val pFunction by cachedReference(PikaAPI.PFunction)
    val pAnnotation by cachedReference(PikaAPI.PAnnotation)
    val pIntrospectionData by cachedReference(PikaAPI.PIntrospectionData)
  }

  val kotlinStd = _KotlinStd()

  inner class _KotlinStd {
    val collections = _Collections()

    inner class _Collections {
      val listOf by cachedFunction(KotlinStd.Collections.listOf)
      val emptyList by cachedFunction(KotlinStd.Collections.emptyList)
      val emptyMap by cachedFunction(KotlinStd.Collections.emptyMap)
      val mapOf by cachedFunction(KotlinStd.Collections.mapOf)
    }

    val pair by cachedReference(KotlinStd.pair)
    val to by cachedFunction(KotlinStd.to)
  }

  private fun cachedReference(classId: ClassId) = lazy {
    classResolver(classId) ?: error("Class $classId not found")
  }

  private fun cachedFunction(callableId: CallableId) = lazy {
    functionResolver(callableId).takeIf { it.isNotEmpty() } ?: error("Function $callableId not found")
  }
}
