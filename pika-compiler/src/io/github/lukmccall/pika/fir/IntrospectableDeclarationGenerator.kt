package io.github.lukmccall.pika.fir

import io.github.lukmccall.pika.symbols.PikaAPI
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

/**
 * FIR extension that generates `__PIntrospectionData()` function declarations
 * for classes that implement the Introspectable marker interface.
 *
 * This makes the function visible during FIR resolution so calls to it don't fail
 * with "unresolved reference" errors. The actual implementation is generated
 * during the IR phase by IntrospectableTransformer.
 */
class IntrospectableDeclarationGenerator(
  session: FirSession
) : FirDeclarationGenerationExtension(session) {
  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext
  ): Set<Name> {
    return if (classSymbol.implementsIntrospectable(session)) {
      setOf(PikaAPI.Names.IntrospectionDataFunction)
    } else {
      emptySet()
    }
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    if (callableId.callableName != PikaAPI.Names.IntrospectionDataFunction) {
      return emptyList()
    }

    val owner = context?.owner ?: return emptyList()
    if (!owner.implementsIntrospectable(session)) {
      return emptyList()
    }

    // Generate function: fun __PIntrospectionData(): PIntrospectionData<OwnerType>
    // Get the owner class type to use as type argument
    val ownerClassType = owner.defaultType()
    val returnType = PikaAPI.PIntrospectionData.constructClassLikeType(
      arrayOf(ownerClassType)
    )

    val function = createMemberFunction(
      owner,
      key = IntrospectableDeclarationGeneratorKey,
      name = PikaAPI.Names.IntrospectionDataFunction,
      returnType = returnType
    ) {
      // Make it open so subclasses can override with covariant return type
      modality = if (owner.modality == Modality.FINAL) {
        Modality.FINAL
      } else {
        Modality.OPEN
      }
    }

    return listOf(function.symbol)
  }
}

object IntrospectableDeclarationGeneratorKey : org.jetbrains.kotlin.GeneratedDeclarationKey()
