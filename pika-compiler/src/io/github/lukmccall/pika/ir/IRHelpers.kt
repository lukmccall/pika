@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.lukmccall.pika.ir

import io.github.lukmccall.pika.Identifiers
import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId

fun Collection<IrSimpleFunctionSymbol>.firstSingleVarargsArgument(): IrSimpleFunctionSymbol {
  return first { func ->
    func.owner.parameters.count { it.varargElementType != null } == 1 &&
      func.owner.parameters.size == 1
  }
}

/**
 * Class(
 *   a = 10,
 *   b = "pika",
 *   c = String::class.java
 * ) -> mapOf(
 *   "a" to 10,
 *   "b" to "pika",
 *   "c" to String::class.java
 * )
 */
fun IrConstructorCall.toArgsMap(
  poet: IRPoet
): IrExpression {
  val constructor = symbol.owner
  val args = mutableListOf<IrExpression>()

  val parameters = constructor.parameters.filter { it.kind == IrParameterKind.Regular }

  arguments.zip(parameters.map { it.name.toString() }).forEach { (argExpression, name) ->
    val copiedArg = argExpression.constCopy(poet)
    if (copiedArg != null) {
      args.add(
        poet.kotlin.pair(
          keyType = poet.irBuiltIns.stringType,
          key = poet.kotlin.string(name),
          valueType = poet.irBuiltIns.anyNType,
          value = copiedArg
        )
      )
    }
  }

  return if (args.isEmpty()) {
    poet.kotlin.emptyMap(poet.irBuiltIns.stringType, poet.irBuiltIns.anyNType)
  } else {
    poet.kotlin.mapOf(
      keyType = poet.irBuiltIns.stringType,
      valueType = poet.irBuiltIns.anyNType,
      args
    )
  }
}

/**
 * Duplicates expression. Works only for const, enums and Class<*>
 */
private fun IrExpression?.constCopy(
  poet: IRPoet
): IrExpression? {
  if (this == null) {
    return null
  }

  return when (this) {
    is IrConst -> {
      createIrConst(
        startOffset = -1,
        endOffset = -1,
        type = type,
        kind = kind,
        value = value
      )
    }

    is IrGetEnumValue -> {
      poet.kotlin.string(symbol.owner.name.asString())
    }

    is IrClassReferenceImpl -> {
      poet.kotlin.javaClass(symbol as IrClassSymbol)
    }

    else -> null
  }
}

fun IrClass.pikaObject(): IrClass? = declarations
  .filterIsInstance<IrClass>()
  .find { it.name.asString() == Identifiers.PIKA_NESTED_OBJECT_NAME }

fun IrClass.hasIntrospectableAnnotation(extraAnnotationClassIds: Set<ClassId> = emptySet()): Boolean {
  return hasAnnotation(PikaAPI.Introspectable) || extraAnnotationClassIds.any { hasAnnotation(it) }
}
