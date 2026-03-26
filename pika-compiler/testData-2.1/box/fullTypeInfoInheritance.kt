// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

open class BaseClass(open val baseField: String)

interface TestInterface

class Person(
  override val baseField: String,
  val name: String
) : BaseClass(baseField), TestInterface

fun box(): String {
  val fullPersonInfo = fullTypeInfo<Person>()

  // Test baseClass
  val baseClass = fullPersonInfo.baseClass
    ?: return "FAIL: baseClass should not be null"
  if (baseClass.className != "test.BaseClass") return "FAIL: baseClass className expected test.BaseClass but got ${baseClass.className}"
  if (baseClass.kClass != BaseClass::class) return "FAIL: baseClass kClass"
  // BaseClass should have 1 field: baseField
  if (baseClass.fields.size != 1) return "FAIL: baseClass should have 1 field but got ${baseClass.fields.size}"
  if (baseClass.fields[0].name != "baseField") return "FAIL: baseClass field should be baseField"

  // Test interfaces
  if (fullPersonInfo.interfaces.size != 1) return "FAIL: should have 1 interface but got ${fullPersonInfo.interfaces.size}"
  if (fullPersonInfo.interfaces[0] != TestInterface::class) return "FAIL: interface should be TestInterface::class"

  return "OK"
}
