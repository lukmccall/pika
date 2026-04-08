// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test String
  val stringInfo = pTypeDescriptorOf<String>()
  if (stringInfo !is PTypeDescriptor.Concrete) return "FAIL: String should be Concrete"
  if (stringInfo.pType.kClass != String::class) return "FAIL: String kClass"
  if (stringInfo.isNullable) return "FAIL: String should not be nullable"

  // Test Int
  val intInfo = pTypeDescriptorOf<Int>()
  if (intInfo !is PTypeDescriptor.Concrete) return "FAIL: Int should be Concrete"
  if (intInfo.pType.kClass != Int::class) return "FAIL: Int kClass"
  if (intInfo.isNullable) return "FAIL: Int should not be nullable"

  // Test Boolean
  val boolInfo = pTypeDescriptorOf<Boolean>()
  if (boolInfo !is PTypeDescriptor.Concrete) return "FAIL: Boolean should be Concrete"
  if (boolInfo.pType.kClass != Boolean::class) return "FAIL: Boolean kClass"

  // Test nullable Int
  val nullableIntInfo = pTypeDescriptorOf<Int?>()
  if (nullableIntInfo !is PTypeDescriptor.Concrete) return "FAIL: Int? should be Concrete"
  if (nullableIntInfo.pType.kClass != Int::class) return "FAIL: Int? kClass"
  if (!nullableIntInfo.isNullable) return "FAIL: Int? should be nullable"

  // Test nullable String
  val nullableStringInfo = pTypeDescriptorOf<String?>()
  if (nullableStringInfo !is PTypeDescriptor.Concrete) return "FAIL: String? should be Concrete"
  if (nullableStringInfo.pType.kClass != String::class) return "FAIL: String? kClass"
  if (!nullableStringInfo.isNullable) return "FAIL: String? should be nullable"

  return "OK"
}
