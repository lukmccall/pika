package io.github.lukmccall.pika.runners

import io.github.lukmccall.pika.services.configurePlugin
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.AsmLikeInstructionListingHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractJvmBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
  override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
    return EnvironmentBasedStandardLibrariesPathProvider
  }

  override fun configure(builder: TestConfigurationBuilder) = with(builder) {
    super.configure(this)
    /*
     * Containers of different directives, which can be used in tests:
     * - ModuleStructureDirectives
     * - LanguageSettingsDirectives
     * - DiagnosticsDirectives
     * - FirDiagnosticsDirectives
     * - CodegenTestDirectives
     * - JvmEnvironmentConfigurationDirectives
     * - AsmLikeInstructionListingDirectives
     *
     * All of them are located in `org.jetbrains.kotlin.test.directives` package
     */
    defaultDirectives {
      +CodegenTestDirectives.DUMP_IR
      +FirDiagnosticsDirectives.FIR_DUMP
      +JvmEnvironmentConfigurationDirectives.FULL_JDK

      +CodegenTestDirectives.IGNORE_DEXING // Avoids loading R8 from the classpath.
    }

    useDirectives(AsmLikeInstructionListingDirectives)
    configureJvmArtifactsHandlersStep {
      useHandlers(::AsmLikeInstructionListingHandler)
    }

    configurePlugin()
  }
}
