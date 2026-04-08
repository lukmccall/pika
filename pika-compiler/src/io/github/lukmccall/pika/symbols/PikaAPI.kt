package io.github.lukmccall.pika.symbols

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

  val FullTypeInfo = ClassId(API_PACKAGE, Name.identifier("FullTypeInfo"))
  val FullFieldInfo = ClassId(API_PACKAGE, Name.identifier("FullFieldInfo"))
  val AnnotationInfo = ClassId(API_PACKAGE, Name.identifier("AnnotationInfo"))
  val Visibility = ClassId(API_PACKAGE, Name.identifier("Visibility"))
}
