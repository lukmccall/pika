package io.github.lukmccall.pika.symbols

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KotlinStd {
  object Collections {
    val listOf = CallableId(FqName("kotlin.collections"), Name.identifier("listOf"))
    val emptyMap = CallableId(FqName("kotlin.collections"), Name.identifier("emptyMap"))
    val mapOf = CallableId(FqName("kotlin.collections"), Name.identifier("mapOf"))
  }

  val pair = ClassId(FqName("kotlin"), Name.identifier("Pair"))
  val to = CallableId(FqName("kotlin"), Name.identifier("to"))
}
