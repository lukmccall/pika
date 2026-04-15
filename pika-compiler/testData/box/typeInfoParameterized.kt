// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test List<String>
  val listStringInfo = typeDescriptorOf<List<String>>()
  if (listStringInfo !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: List<String> should be Parameterized"
  if (listStringInfo.pType.jClass != List::class.java) return "FAIL: List<String> kClass"
  if (listStringInfo.isNullable) return "FAIL: List<String> should not be nullable"
  if (listStringInfo.argumentsPTypes.size != 1) return "FAIL: List<String> should have 1 type argument"

  val listArg = listStringInfo.argumentsPTypes[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: List<String> arg should be Concrete"
  if (listArg.pType.jClass != String::class.java) return "FAIL: List<String> arg kClass"

  // Test Map<String, Int>
  val mapInfo = typeDescriptorOf<Map<String, Int>>()
  if (mapInfo !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: Map<String, Int> should be Parameterized"
  if (mapInfo.pType.jClass != Map::class.java) return "FAIL: Map<String, Int> kClass"
  if (mapInfo.argumentsPTypes.size != 2) return "FAIL: Map<String, Int> should have 2 type arguments"

  val mapKeyArg = mapInfo.argumentsPTypes[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: Map key should be Concrete"
  if (mapKeyArg.pType.jClass != String::class.java) return "FAIL: Map key kClass"

  val mapValueArg = mapInfo.argumentsPTypes[1] as? PTypeDescriptor.Concrete
    ?: return "FAIL: Map value should be Concrete"
  if (mapValueArg.pType.jClass != Int::class.java) return "FAIL: Map value kClass"

  return "OK"
}
