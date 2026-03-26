// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test List<String>
  val listStringInfo = typeInfo<List<String>>()
  if (listStringInfo !is TypeInfo.Parameterized) return "FAIL: List<String> should be Parameterized"
  if (listStringInfo.typeName != "kotlin.collections.List") return "FAIL: List<String> typeName expected kotlin.collections.List but got ${listStringInfo.typeName}"
  if (listStringInfo.kClass != List::class) return "FAIL: List<String> kClass"
  if (listStringInfo.isNullable) return "FAIL: List<String> should not be nullable"
  if (listStringInfo.typeArguments.size != 1) return "FAIL: List<String> should have 1 type argument"

  val listArg = listStringInfo.typeArguments[0] as? TypeInfo.Simple
    ?: return "FAIL: List<String> arg should be Simple"
  if (listArg.typeName != "kotlin.String") return "FAIL: List<String> arg typeName"
  if (listArg.kClass != String::class) return "FAIL: List<String> arg kClass"

  // Test Map<String, Int>
  val mapInfo = typeInfo<Map<String, Int>>()
  if (mapInfo !is TypeInfo.Parameterized) return "FAIL: Map<String, Int> should be Parameterized"
  if (mapInfo.typeName != "kotlin.collections.Map") return "FAIL: Map<String, Int> typeName expected kotlin.collections.Map but got ${mapInfo.typeName}"
  if (mapInfo.kClass != Map::class) return "FAIL: Map<String, Int> kClass"
  if (mapInfo.typeArguments.size != 2) return "FAIL: Map<String, Int> should have 2 type arguments"

  val mapKeyArg = mapInfo.typeArguments[0] as? TypeInfo.Simple
    ?: return "FAIL: Map key should be Simple"
  if (mapKeyArg.typeName != "kotlin.String") return "FAIL: Map key typeName"

  val mapValueArg = mapInfo.typeArguments[1] as? TypeInfo.Simple
    ?: return "FAIL: Map value should be Simple"
  if (mapValueArg.typeName != "kotlin.Int") return "FAIL: Map value typeName"

  return "OK"
}
