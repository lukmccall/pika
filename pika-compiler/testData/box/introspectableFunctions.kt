// WITH_STDLIB
package test

import io.github.lukmccall.pika.*

@Introspectable
class Calculator {
  val value: Int = 0

  fun add(a: Int, b: Int): Int = a + b

  private fun multiply(a: Int, b: Int): Int = a * b

  internal fun subtract(a: Int, b: Int): Int = a - b
}

fun box(): String {
  val calc = Calculator()
  val data = introspectionOf<Calculator>()

  // Check functions
  if (data.functions.isEmpty()) return "FAIL: should have functions"

  val addFunc = data.functions.find { it.name == "add" }
    ?: return "FAIL: add function not found"
  if (addFunc.visibility != PVisibility.PUBLIC) return "FAIL: add should be PUBLIC"

  val multiplyFunc = data.functions.find { it.name == "multiply" }
    ?: return "FAIL: multiply function not found"
  if (multiplyFunc.visibility != PVisibility.PRIVATE) return "FAIL: multiply should be PRIVATE"

  val subtractFunc = data.functions.find { it.name == "subtract" }
    ?: return "FAIL: subtract function not found"
  if (subtractFunc.visibility != PVisibility.INTERNAL) return "FAIL: subtract should be INTERNAL"

  return "OK"
}
