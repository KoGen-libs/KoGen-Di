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

    fun setPackageName(paramsPackageName: String?, components: List<KSDeclaration>) {
        packageName = paramsPackageName?.plus(".di").takeIf {
            !it.isNullOrEmpty()
        } ?: run {
            val packageParts = components.firstOrNull()?.packageName?.asString()?.split(".")
            packageParts?.subList(0, 3)?.joinToString(".")?.plus(".di") ?: "kz.evko.kogen_di"
        }
    }

    fun createBeansList(beans: List<KSFunctionDeclaration>) {
        try {
            logger.info("Creating beans list")

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
        } catch (e: Exception) {
            logger.info("Exception: ${e.message}")
        }
    }

    fun createComponentList(components: List<KSClassDeclaration>) {
        try {
            logger.info("Creating component list")
            logger.info("Components count: ${components.size}")

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
        } catch (e: Exception) {
            logger.info("Exception: ${e.message}")
        }
    }

    fun createViewModelList(viewModels: List<KSClassDeclaration>) {
        try {
            logger.info("Creating view model list")
            logger.info("View models count: ${viewModels.size}")

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
        } catch (e: Exception) {
            logger.info("Exception: ${e.message}")
        }
    }

    fun createInjectFactory(includeViewModelInjector: Boolean, includeFragmentInjector: Boolean) {
        try {
            logger.info("Creating component factory")

            val generator = InjectFactoryGenerator(packageName)
            val content = generator.generateInjectors(
                includeViewModelInjector = includeViewModelInjector,
                includeFragmentInjector = includeFragmentInjector,
            )

            val file: OutputStream =
                createFile(emptyList(), "KoGenInjectors")
            file += content
            file.close()
        } catch (e: Exception) {
            logger.info("Exception: ${e.message}")
        }
    }

    private fun createFile(
        files: List<KSFile>,
        fileName: String,
    ) = codeGenerator.createNewFile(
        Dependencies(
            true,
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