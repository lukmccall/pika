package io.github.lukmccall.pika.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object PikaAPI {
  private val API_PACKAGE = FqName("io.github.lukmccall.pika")

  object TypeInfo {
    val Root = ClassId(API_PACKAGE, Name.identifier("TypeInfo"))

    val Simple = ClassId(API_PACKAGE, FqName("TypeInfo.Simple"), isLocal = false)
    val Parameterized = ClassId(API_PACKAGE, FqName("TypeInfo.Parameterized"), isLocal = false)
    val Star = ClassId(API_PACKAGE, FqName("TypeInfo.Star"), isLocal = false)
  }

  val FullTypeInfo = ClassId(API_PACKAGE, Name.identifier("FullTypeInfo"))
  val FullFieldInfo = ClassId(API_PACKAGE, Name.identifier("FullFieldInfo"))
  val AnnotationInfo = ClassId(API_PACKAGE, Name.identifier("AnnotationInfo"))
  val Visibility = ClassId(API_PACKAGE, Name.identifier("Visibility"))
}
