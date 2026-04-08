package io.github.lukmccall.pika

object Identifiers {
  const val PACKAGE_NAME = "io.github.lukmccall.pika"
  const val P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "pTypeDescriptorOf"
  const val FULL_TYPE_INFO_FUNCTION_NAME = "fullTypeInfo"
  const val P_INTROSPECTION_OF_FUNCTION_NAME = "pIntrospectionOf"

  fun String.withPackageName(): String {
    return "$PACKAGE_NAME.$this"
  }

  fun String.removePackageName(): String {
    return this.removePrefix("$PACKAGE_NAME.")
  }
}
