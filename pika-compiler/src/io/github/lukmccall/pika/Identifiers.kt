package io.github.lukmccall.pika

import org.jetbrains.kotlin.name.FqName

// The multi dollar syntax isn't available in kotlin 2.1.20
@Suppress("CanConvertToMultiDollarString", "CanUnescapeDollarLiteral")
object Identifiers {
  const val PACKAGE_NAME = "io.github.lukmccall.pika"

  const val P_TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "pTypeDescriptorOf"
  const val P_INTROSPECTION_OF_FUNCTION_NAME = "pIntrospectionOf"
  const val P_IS_INTROSPECTABLE_FUNCTION_NAME = "pIsIntrospectable"

  const val P_INTROSPECTION_DATA_FUNCTION_NAME = "__PIntrospectionData"
  const val INTROSPECTABLE_INTERFACE_NAME = "Introspectable"

  // Synthetic accessor function name prefixes for backing field access
  private const val SYNTHETIC_GETTER_PREFIX = "__pika\$get\$"
  private const val SYNTHETIC_SETTER_PREFIX = "__pika\$set\$"

  fun syntheticGetterName(propertyName: String): String = "$SYNTHETIC_GETTER_PREFIX$propertyName"
  fun syntheticSetterName(propertyName: String): String = "$SYNTHETIC_SETTER_PREFIX$propertyName"

  fun isSyntheticAccessor(name: String): Boolean =
    isSyntheticGetter(name) || isSyntheticSetter(name)

  fun isSyntheticGetter(name: String): Boolean =
    name.startsWith(SYNTHETIC_GETTER_PREFIX)

  fun isSyntheticSetter(name: String): Boolean =
    name.startsWith(SYNTHETIC_SETTER_PREFIX)

  fun removeSyntheticAccessor(name: String): String =
    name.removePrefix(SYNTHETIC_SETTER_PREFIX).removePrefix(SYNTHETIC_GETTER_PREFIX)

  fun String.withPackageName(): String {
    return "$PACKAGE_NAME.$this"
  }

  fun String.removePackageName(): String {
    return this.removePrefix("$PACKAGE_NAME.")
  }

  fun String.toFq(): FqName = FqName(this)
}
