package kz.evko.kogen_di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kz.evko.kogen_di.contentGenerator.BeansListGenerator
import kz.evko.kogen_di.contentGenerator.ComponentListGenerator
import kz.evko.kogen_di.contentGenerator.InjectFactoryGenerator
import kz.evko.kogen_di.contentGenerator.ViewModelListGenerator
import java.io.OutputStream

internal class FileWriter(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private var packageName = ""

    fun setPackageName(components: List<KSDeclaration>) {
        if (components.isNotEmpty()) packageName = components.first().packageName.asString()
    }

    fun createBeansList(beans: List<KSFunctionDeclaration>) {
        logger.warn("Creating beans list")
        logger.warn("Beans count: ${beans.size}")

        val generator = BeansListGenerator(logger, packageName)
        val listContent = generator.generateBeansList(beans)

        val file: OutputStream =
            createFile(beans.toFileList(), "KoGenBeansImpl")
        file += listContent
        file.close()

        val factoryContent = generator.generateBeansFactory(beans)
        val factoryFile: OutputStream =
            createFile(beans.toFileList(), "KoGenBeansFactoryImpl")
        factoryFile += factoryContent
        factoryFile.close()
    }

    fun createComponentList(components: List<KSClassDeclaration>) {
        logger.warn("Creating component list")
        logger.warn("Components count: ${components.size}")

        val generator = ComponentListGenerator(logger, packageName)
        val content = generator.generateComponentList(components)

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenComponentsImpl")
        file += content
        file.close()

        val factoryContent = generator.createComponentFactory()
        val factoryFile: OutputStream =
            createFile(components.toFileList(), "KoGenComponentsFactoryImpl")
        factoryFile += factoryContent
        factoryFile.close()

        createInjectFactory(components)
    }

    fun createViewModelList(viewModels: List<KSClassDeclaration>) {
        logger.warn("Creating view model list")
        logger.warn("View models count: ${viewModels.size}")

        val generator = ViewModelListGenerator(logger, packageName)
        val content = generator.generateViewModelList(viewModels)

        val file: OutputStream =
            createFile(viewModels.toFileList(), "KoGenViewModelsImpl")
        file += content
        file.close()

        val factoryContent = generator.generateViewModelFactory()
        val factoryFile: OutputStream =
            createFile(viewModels.toFileList(), "KoGenViewModelScopeImpl")
        factoryFile += factoryContent
        factoryFile.close()
    }

    private fun createInjectFactory(components: List<KSClassDeclaration>) {
        logger.warn("Creating component factory")

        val generator = InjectFactoryGenerator(packageName)
        val content = generator.generateBeansList()

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenInjectors")
        file += content
        file.close()
    }

    private fun createFile(
        files: List<KSFile>,
        fileName: String,
    ) = codeGenerator.createNewFile(
        Dependencies(
            false,
            *files.toList().toTypedArray(),
        ),
        packageName,
        fileName
    )
}

internal operator fun OutputStream.plusAssign(text: String) {
    write(text.toByteArray())
}

internal fun List<KSDeclaration>.toFileList(): List<KSFile> =
    mapNotNull { it.containingFile }