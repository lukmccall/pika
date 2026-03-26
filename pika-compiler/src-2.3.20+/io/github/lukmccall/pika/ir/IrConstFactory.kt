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
): IrConst = IrConstImpl(startOffset, endOffset, type, kind, value)
