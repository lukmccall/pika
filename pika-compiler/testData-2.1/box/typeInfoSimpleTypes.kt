// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test String
  val stringInfo = typeInfo<String>()
  if (stringInfo !is TypeInfo.Simple) return "FAIL: String should be Simple"
  if (stringInfo.typeName != "kotlin.String") return "FAIL: String typeName expected kotlin.String but got ${stringInfo.typeName}"
  if (stringInfo.kClass != String::class) return "FAIL: String kClass"
  if (stringInfo.isNullable) return "FAIL: String should not be nullable"

  // Test Int
  val intInfo = typeInfo<Int>()
  if (intInfo !is TypeInfo.Simple) return "FAIL: Int should be Simple"
  if (intInfo.typeName != "kotlin.Int") return "FAIL: Int typeName expected kotlin.Int but got ${intInfo.typeName}"
  if (intInfo.kClass != Int::class) return "FAIL: Int kClass"
  if (intInfo.isNullable) return "FAIL: Int should not be nullable"

  // Test Boolean
  val boolInfo = typeInfo<Boolean>()
  if (boolInfo !is TypeInfo.Simple) return "FAIL: Boolean should be Simple"
  if (boolInfo.typeName != "kotlin.Boolean") return "FAIL: Boolean typeName expected kotlin.Boolean but got ${boolInfo.typeName}"
  if (boolInfo.kClass != Boolean::class) return "FAIL: Boolean kClass"

  // Test nullable Int
  val nullableIntInfo = typeInfo<Int?>()
  if (nullableIntInfo !is TypeInfo.Simple) return "FAIL: Int? should be Simple"
  if (nullableIntInfo.typeName != "kotlin.Int") return "FAIL: Int? typeName expected kotlin.Int but got ${nullableIntInfo.typeName}"
  if (nullableIntInfo.kClass != Int::class) return "FAIL: Int? kClass"
  if (!nullableIntInfo.isNullable) return "FAIL: Int? should be nullable"

  // Test nullable String
  val nullableStringInfo = typeInfo<String?>()
  if (nullableStringInfo !is TypeInfo.Simple) return "FAIL: String? should be Simple"
  if (nullableStringInfo.typeName != "kotlin.String") return "FAIL: String? typeName"
  if (!nullableStringInfo.isNullable) return "FAIL: String? should be nullable"

  return "OK"
}
