// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test nested generics: Map<String, List<Int?>>
  val nestedInfo = typeInfo<Map<String, List<Int?>>>()
  if (nestedInfo !is TypeInfo.Parameterized) return "FAIL: nested should be Parameterized"
  if (nestedInfo.typeName != "kotlin.collections.Map") return "FAIL: nested typeName"
  if (nestedInfo.typeArguments.size != 2) return "FAIL: nested should have 2 type arguments"

  val nestedKeyArg = nestedInfo.typeArguments[0] as? TypeInfo.Simple
    ?: return "FAIL: nested key should be Simple"
  if (nestedKeyArg.typeName != "kotlin.String") return "FAIL: nested key typeName"

  val nestedValueArg = nestedInfo.typeArguments[1] as? TypeInfo.Parameterized
    ?: return "FAIL: nested value should be Parameterized"
  if (nestedValueArg.typeName != "kotlin.collections.List") return "FAIL: nested value typeName"
  if (nestedValueArg.typeArguments.size != 1) return "FAIL: nested value should have 1 type arg"

  val nestedListArg = nestedValueArg.typeArguments[0] as? TypeInfo.Simple
    ?: return "FAIL: nested list arg should be Simple"
  if (nestedListArg.typeName != "kotlin.Int") return "FAIL: nested list arg typeName"
  if (!nestedListArg.isNullable) return "FAIL: nested list arg should be nullable"

  return "OK"
}
