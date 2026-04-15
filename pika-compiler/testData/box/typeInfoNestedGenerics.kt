// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

fun box(): String {
  // Test nested generics: Map<String, List<Int?>>
  val nestedInfo = typeDescriptorOf<Map<String, List<Int?>>>()
  if (nestedInfo !is PTypeDescriptor.Concrete.Parameterized) return "FAIL: nested should be Parameterized"
  if (nestedInfo.pType.jClass != Map::class.java) return "FAIL: nested kClass"
  if (nestedInfo.parameters.size != 2) return "FAIL: nested should have 2 type arguments"

  val nestedKeyArg = nestedInfo.parameters[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: nested key should be Concrete"
  if (nestedKeyArg.pType.jClass != String::class.java) return "FAIL: nested key kClass"

  val nestedValueArg = nestedInfo.parameters[1] as? PTypeDescriptor.Concrete.Parameterized
    ?: return "FAIL: nested value should be Parameterized"
  if (nestedValueArg.pType.jClass != List::class.java) return "FAIL: nested value kClass"
  if (nestedValueArg.parameters.size != 1) return "FAIL: nested value should have 1 type arg"

  val nestedListArg = nestedValueArg.parameters[0] as? PTypeDescriptor.Concrete
    ?: return "FAIL: nested list arg should be Concrete"
  if (nestedListArg.pType.jClass != Int::class.java) return "FAIL: nested list arg kClass"
  if (!nestedListArg.isNullable) return "FAIL: nested list arg should be nullable"

  return "OK"
}
