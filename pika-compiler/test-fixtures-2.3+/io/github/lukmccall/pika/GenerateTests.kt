package io.github.lukmccall.pika

import io.github.lukmccall.pika.runners.AbstractJvmBoxTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
  generateTestGroupSuiteWithJUnit5 {
    testGroup(testDataRoot = "pika-compiler/testData", testsRoot = "pika-compiler/test-gen") {
      testClass<AbstractJvmBoxTest> {
        model("box")
      }
    }
  }
}
