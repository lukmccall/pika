package io.github.lukmccall.pika.symbols

import io.github.lukmccall.pika.Identifiers
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object PikaAPI {
  private val API_PACKAGE = FqName("io.github.lukmccall.pika")

  val PType = ClassId(API_PACKAGE, Name.identifier("PType"))

  object PTypeDescriptor {
    val Root = ClassId(API_PACKAGE, Name.identifier("PTypeDescriptor"))

    val Concrete = ClassId(API_PACKAGE, FqName("PTypeDescriptor.Concrete"), isLocal = false)
    val Parameterized = ClassId(API_PACKAGE, FqName("PTypeDescriptor.Concrete.Parameterized"), isLocal = false)
    val Star = ClassId(API_PACKAGE, FqName("PTypeDescriptor.Star"), isLocal = false)
  }

  val PTypeDescriptorRegistry = ClassId(API_PACKAGE, Name.identifier(Identifiers.P_TYPE_DESCRIPTOR_REGISTRY_CLASS))

  // Introspectable types
  val Introspectable = ClassId(API_PACKAGE, Name.identifier(Identifiers.INTROSPECTABLE_INTERFACE_NAME))
  val PVisibility = ClassId(API_PACKAGE, Name.identifier("PVisibility"))
  val PProperty = ClassId(API_PACKAGE, Name.identifier("PProperty"))
  val PFunction = ClassId(API_PACKAGE, Name.identifier("PFunction"))
  val PAnnotation = ClassId(API_PACKAGE, Name.identifier("PAnnotation"))
  val PIntrospectionData = ClassId(API_PACKAGE, Name.identifier("PIntrospectionData"))

  object Names {
    val IntrospectionDataFunction = Name.identifier(Identifiers.P_INTROSPECTION_DATA_FUNCTION_NAME)
  }
}
