// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test String
  val stringInfo = typeDescriptorOf<String>()
  if (stringInfo !is PTypeDescriptor.Concrete) return "FAIL: String should be Concrete"
  if (stringInfo.pType.jClass != String::class.java) return "FAIL: String kClass"
  if (stringInfo.isNullable) return "FAIL: String should not be nullable"

  // Test Int
  val intInfo = typeDescriptorOf<Int>()
  if (intInfo !is PTypeDescriptor.Concrete) return "FAIL: Int should be Concrete"
  if (intInfo.pType.jClass != Int::class.java) return "FAIL: Int kClass"
  if (intInfo.isNullable) return "FAIL: Int should not be nullable"

  // Test Boolean
  val boolInfo = typeDescriptorOf<Boolean>()
  if (boolInfo !is PTypeDescriptor.Concrete) return "FAIL: Boolean should be Concrete"
  if (boolInfo.pType.jClass != Boolean::class.java) return "FAIL: Boolean kClass"

  // Test nullable Int
  val nullableIntInfo = typeDescriptorOf<Int?>()
  if (nullableIntInfo !is PTypeDescriptor.Concrete) return "FAIL: Int? should be Concrete"
  if (nullableIntInfo.pType.jClass != Int::class.java) return "FAIL: Int? kClass"
  if (!nullableIntInfo.isNullable) return "FAIL: Int? should be nullable"

  // Test nullable String
  val nullableStringInfo = typeDescriptorOf<String?>()
  if (nullableStringInfo !is PTypeDescriptor.Concrete) return "FAIL: String? should be Concrete"
  if (nullableStringInfo.pType.jClass != String::class.java) return "FAIL: String? kClass"
  if (!nullableStringInfo.isNullable) return "FAIL: String? should be nullable"

  return "OK"
}
