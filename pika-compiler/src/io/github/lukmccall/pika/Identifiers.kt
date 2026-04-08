package io.github.lukmccall.pika

import org.jetbrains.kotlin.name.FqName

object Identifiers {
  const val PACKAGE_NAME = "io.github.lukmccall.pika"

  const val P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "pTypeDescriptorOf"
  const val P_INTROSPECTION_OF_FUNCTION_NAME = "pIntrospectionOf"

  const val P_INTROSPECTION_DATA_FUNCTION_NAME = "__PIntrospectionData"
  const val INTROSPECTABLE_INTERFACE_NAME = "Introspectable"

  fun String.withPackageName(): String {
    return "$PACKAGE_NAME.$this"
  }

  fun String.removePackageName(): String {
    return this.removePrefix("$PACKAGE_NAME.")
  }

  fun String.toFq(): FqName = FqName(this)
}

