package io.github.lukmccall.pika

import org.jetbrains.kotlin.name.FqName

// The multi dollar syntax isn't available in kotlin 2.1.20
@Suppress("CanConvertToMultiDollarString", "CanUnescapeDollarLiteral")
object Identifiers {
  const val PACKAGE_NAME = "io.github.lukmccall.pika"
  val PACKAGE_NAME_JAVE_NOTATION = PACKAGE_NAME.replace(".", "/")

  const val TYPE_DESCRIPTOR_OF_FUNCTION_NAME = "typeDescriptorOf"
  const val INTROSPECTION_OF_FUNCTION_NAME = "introspectionOf"
  const val IS_INTROSPECTABLE_FUNCTION_NAME = "isIntrospectable"

  const val INTROSPECTION_DATA_FIELD_NAME = "__pika\$IntrospectionData"

  const val TYPE_DESCRIPTOR_REGISTRY_CLASS = "PTypeDescriptorRegistry"
  const val TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_CONCRETE = "getOrCreateConcrete"
  const val TYPE_DESCRIPTOR_REGISTRY_GET_OR_CREATE_PARAMETERIZED = "getOrCreateParameterized"
  const val INTROSPECTABLE_INTERFACE_NAME = "Introspectable"

  val SIMPLE_TYPE_FIELD_NAMES: Map<String, Pair<String, String>> = mapOf(
    "kotlin.Int" to ("INT" to "INT_NULLABLE"),
    "kotlin.Long" to ("LONG" to "LONG_NULLABLE"),
    "kotlin.Float" to ("FLOAT" to "FLOAT_NULLABLE"),
    "kotlin.Short" to ("SHORT" to "SHORT_NULLABLE"),
    "kotlin.Double" to ("DOUBLE" to "DOUBLE_NULLABLE"),
    "kotlin.String" to ("STRING" to "STRING_NULLABLE"),
    "kotlin.Boolean" to ("BOOLEAN" to "BOOLEAN_NULLABLE"),
  )

  const val PIKA_SPECIAL_PREFIX = "__pika\$"
  // Synthetic accessor function name prefixes for backing field access
  private const val SYNTHETIC_GETTER_PREFIX = "${PIKA_SPECIAL_PREFIX}get\$"
  private const val SYNTHETIC_SETTER_PREFIX = "${PIKA_SPECIAL_PREFIX}set\$"

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
