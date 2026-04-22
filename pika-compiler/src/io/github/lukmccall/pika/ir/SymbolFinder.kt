package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.Identifiers.toFq
import io.github.lukmccall.pika.symbols.PikaAPI
import io.github.lukmccall.pika.symbols.KotlinStd
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("ClassName")
class SymbolFinder(
  private val classResolver: (ClassId) -> IrClassSymbol?,
  private val functionResolver: (CallableId) -> Collection<IrSimpleFunctionSymbol>
) {
  val pikaAPI = _PikaAPI()

  inner class _PikaAPI {
    val pType by cachedReference(PikaAPI.PType)
    val pTypeDescriptor = _PTypeDescriptor()
    val pTypeDescriptorRegistry = _PTypeDescriptorRegistry()

    inner class _PTypeDescriptorRegistry {
      val classSymbol by cachedReference(PikaAPI.PTypeDescriptorRegistry)

      val getOrCreateConcrete by lazy {
        functionResolver(
          CallableId(
            Identifiers.PACKAGE_NAME.toFq(),
            FqName(Identifiers.P_TYPE_DESCRIPTOR_REGISTRY_CLASS),
            Name.identifier(Identifiers.P_TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_CONCRETE)
          )
        ).firstOrNull() ?: error("Function getOrCreateConcrete not found in PTypeDescriptorRegistry")
      }

      val getOrCreateParameterized by lazy {
        functionResolver(
          CallableId(
            Identifiers.PACKAGE_NAME.toFq(),
            FqName(Identifiers.P_TYPE_DESCRIPTOR_REGISTRY_CLASS),
            Name.identifier(Identifiers.P_TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_PARAMETERIZED)
          )
        ).firstOrNull() ?: error("Function getOrCreateParameterized not found in PTypeDescriptorRegistry")
      }
    }

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

  val javaLangClass by cachedReference(ClassId(FqName("java.lang"), Name.identifier("Class")))

  private fun cachedReference(classId: ClassId) = lazy {
    classResolver(classId) ?: error("Class $classId not found")
  }

  private fun cachedFunction(callableId: CallableId) = lazy {
    functionResolver(callableId).takeIf { it.isNotEmpty() } ?: error("Function $callableId not found")
  }
}
