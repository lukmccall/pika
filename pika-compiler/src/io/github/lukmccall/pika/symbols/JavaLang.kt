package io.github.lukmccall.pika.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object JavaLang {
  val indexOutOfBoundsException = ClassId(FqName("java.lang"), Name.identifier("IndexOutOfBoundsException"))
  val unsupportedOperationException = ClassId(FqName("java.lang"), Name.identifier("UnsupportedOperationException"))
}
