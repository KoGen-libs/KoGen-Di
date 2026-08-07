package kz.evko.kogen_di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ksp.writeTo
import kz.evko.kogen_di.contentGenerator.BeansListGenerator
import kz.evko.kogen_di.contentGenerator.ComponentListGenerator
import kz.evko.kogen_di.contentGenerator.InjectFactoryGenerator
import kz.evko.kogen_di.contentGenerator.ViewModelListGenerator

internal class FileWriter(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private var packageName = ""

    fun setPackageName(paramsPackageName: String?, components: List<KSDeclaration>) {
        packageName = paramsPackageName?.plus(".di").takeIf {
            !it.isNullOrEmpty()
        } ?: run {
            val packageParts = components.firstOrNull()?.packageName?.asString()?.split(".")
            packageParts?.subList(0, 3)?.joinToString(".")?.plus(".di") ?: "kz.evko.kogen_di"
        }
    }

    fun createBeansList(beans: List<KSFunctionDeclaration>) {
        logger.info("Creating beans list")

        val generator = BeansListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *beans.toFileList().toTypedArray())

        generator.generateBeansList(beans).writeTo(codeGenerator, dependencies)
        generator.generateBeansFactory(beans).writeTo(codeGenerator, dependencies)
    }

    fun createComponentList(components: List<KSClassDeclaration>) {
        logger.info("Creating component list")
        logger.info("Components count: ${components.size}")

        val generator = ComponentListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *components.toFileList().toTypedArray())

        generator.generateComponentList(components).writeTo(codeGenerator, dependencies)
        generator.createComponentFactory(components).writeTo(codeGenerator, dependencies)
    }

    fun createViewModelList(viewModels: List<KSClassDeclaration>) {
        logger.info("Creating view model list")
        logger.info("View models count: ${viewModels.size}")

        val generator = ViewModelListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *viewModels.toFileList().toTypedArray())

        generator.generateViewModelList(viewModels).writeTo(codeGenerator, dependencies)
        generator.generateViewModelFactory(viewModels).writeTo(codeGenerator, dependencies)
    }

    fun createInjectFactory(includeViewModelInjector: Boolean, includeFragmentInjector: Boolean) {
        logger.info("Creating component factory")

        val generator = InjectFactoryGenerator(packageName)
        generator.generateInjectors(
            includeViewModelInjector = includeViewModelInjector,
            includeFragmentInjector = includeFragmentInjector,
        ).writeTo(codeGenerator, Dependencies(true))
    }
}

internal fun List<KSDeclaration>.toFileList(): List<KSFile> =
    mapNotNull { it.containingFile }
