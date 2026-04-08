// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

class Person(
  val name: String,
  private var age: Int,
  val tags: List<String>
)

fun box(): String {
  val fullPersonInfo = fullTypeInfo<Person>()

  // Test fields count
  if (fullPersonInfo.fields.size != 3) return "FAIL: fullTypeInfo should have 3 fields but got ${fullPersonInfo.fields.size}"

  // Test name field
  val nameFieldInfo = fullPersonInfo.fields.find { it.name == "name" }
    ?: return "FAIL: name not found"
  if (nameFieldInfo.visibility != Visibility.PUBLIC) return "FAIL: name should be PUBLIC"
  if (nameFieldInfo.isMutable) return "FAIL: name should not be mutable"
  val nameFieldType = nameFieldInfo.pTypeDescriptor as? PTypeDescriptor.Concrete
    ?: return "FAIL: name pTypeDescriptor should be Concrete"
  if (nameFieldType.pType.kClass != String::class) return "FAIL: name kClass"

  // Test age field
  val ageFieldInfo = fullPersonInfo.fields.find { it.name == "age" }
    ?: return "FAIL: age not found"
  if (ageFieldInfo.visibility != Visibility.PRIVATE) return "FAIL: age should be PRIVATE but got ${ageFieldInfo.visibility}"
  if (!ageFieldInfo.isMutable) return "FAIL: age should be mutable"
  val ageFieldType = ageFieldInfo.pTypeDescriptor as? PTypeDescriptor.Concrete
    ?: return "FAIL: age pTypeDescriptor should be Concrete"
  if (ageFieldType.pType.kClass != Int::class) return "FAIL: age kClass"

  // Test tags field
  val tagsFieldInfo = fullPersonInfo.fields.find { it.name == "tags" }
    ?: return "FAIL: tags not found"
  if (tagsFieldInfo.visibility != Visibility.PUBLIC) return "FAIL: tags should be PUBLIC"
  if (tagsFieldInfo.isMutable) return "FAIL: tags should not be mutable"
  val tagsFieldType = tagsFieldInfo.pTypeDescriptor as? PTypeDescriptor.Concrete.Parameterized
    ?: return "FAIL: tags pTypeDescriptor should be Parameterized"
  if (tagsFieldType.pType.kClass != List::class) return "FAIL: tags kClass"

  return "OK"
}
