package io.github.lukmccall.pika

object Identifiers {
  const val PACKAGE_NAME = "io.github.lukmccall.pika"
  const val TYPE_INFO_FUNCTION_NAME = "typeInfo"
  const val FULL_TYPE_INFO_FUNCTION_NAME = "fullTypeInfo"

  fun String.withPackageName(): String {
    return "$PACKAGE_NAME.$this"
  }

  fun String.removePackageName(): String {
    return this.removePrefix("$PACKAGE_NAME.")
  }
}

