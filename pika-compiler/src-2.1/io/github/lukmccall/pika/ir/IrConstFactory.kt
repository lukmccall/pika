package io.github.lukmccall.pika.ir

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType

fun createIrConst(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    kind: IrConstKind,
    value: Any?
): IrConst = when (kind) {
    IrConstKind.Null -> IrConstImpl.constNull(startOffset, endOffset, type)
    IrConstKind.Boolean -> IrConstImpl.boolean(startOffset, endOffset, type, value as Boolean)
    IrConstKind.String -> IrConstImpl.string(startOffset, endOffset, type, value as String)
    IrConstKind.Int -> IrConstImpl.int(startOffset, endOffset, type, value as Int)
    IrConstKind.Long -> IrConstImpl.long(startOffset, endOffset, type, value as Long)
    IrConstKind.Float -> IrConstImpl.float(startOffset, endOffset, type, value as Float)
    IrConstKind.Double -> IrConstImpl.double(startOffset, endOffset, type, value as Double)
    IrConstKind.Byte -> IrConstImpl.byte(startOffset, endOffset, type, value as Byte)
    IrConstKind.Short -> IrConstImpl.short(startOffset, endOffset, type, value as Short)
    IrConstKind.Char -> IrConstImpl.char(startOffset, endOffset, type, value as Char)
}
