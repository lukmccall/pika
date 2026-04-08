package io.github.lukmccall.pika.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIR extension that generates `__PIntrospectionData()` function declarations
 * for classes that implement the Introspectable marker interface.
 *
 * This makes the function visible during FIR resolution so calls to it don't fail
 * with "unresolved reference" errors. The actual implementation is generated
 * during the IR phase by IntrospectableTransformer.
 */
class IntrospectableDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

  companion object {
    private val INTROSPECTABLE_CLASS_ID = ClassId(FqName("io.github.lukmccall.pika"), Name.identifier("Introspectable"))
    private val P_INTROSPECTION_DATA_CLASS_ID = ClassId(FqName("io.github.lukmccall.pika"), Name.identifier("PIntrospectionData"))
    private val INTROSPECTION_DATA_FUNCTION_NAME = Name.identifier("__PIntrospectionData")
  }

  override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
    // Check if the class implements Introspectable
    if (!implementsIntrospectable(classSymbol)) {
      return emptySet()
    }
    return setOf(INTROSPECTION_DATA_FUNCTION_NAME)
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?
  ): List<FirNamedFunctionSymbol> {
    if (callableId.callableName != INTROSPECTION_DATA_FUNCTION_NAME) {
      return emptyList()
    }

    val owner = context?.owner ?: return emptyList()
    if (!implementsIntrospectable(owner)) {
      return emptyList()
    }

    // Generate function: fun __PIntrospectionData(): PIntrospectionData<OwnerType>
    // Get the owner class type to use as type argument
    val ownerClassType = owner.defaultType()

    val function = createMemberFunction(
      owner,
      key = IntrospectableDeclarationGeneratorKey,
      name = INTROSPECTION_DATA_FUNCTION_NAME,
      returnType = P_INTROSPECTION_DATA_CLASS_ID.constructClassLikeType(
        arrayOf(ownerClassType)
      )
    ) {
      // Make it open so subclasses can override with covariant return type
      modality = if (owner.modality == Modality.FINAL) Modality.FINAL else Modality.OPEN
    }

    return listOf(function.symbol)
  }

  private fun implementsIntrospectable(classSymbol: FirClassSymbol<*>): Boolean {
    // Check all super types recursively
    return classSymbol.resolvedSuperTypes.any { superType ->
      val superTypeClassId = superType.classId ?: return@any false
      if (superTypeClassId == INTROSPECTABLE_CLASS_ID) {
        return@any true
      }
      // Recursively check parent classes
      val superClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(superTypeClassId) as? FirClassSymbol<*>
      if (superClassSymbol != null) {
        return@any implementsIntrospectable(superClassSymbol)
      }
      false
    }
  }
}

object IntrospectableDeclarationGeneratorKey : org.jetbrains.kotlin.GeneratedDeclarationKey()
