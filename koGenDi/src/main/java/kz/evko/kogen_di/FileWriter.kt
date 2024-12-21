package kz.evko.kogen_di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import kz.evko.kogen_di.contentGenerator.BeansListGenerator
import kz.evko.kogen_di.contentGenerator.InjectFactoryGenerator
import kz.evko.kogen_di.contentGenerator.ComponentListGenerator
import java.io.OutputStream

internal class FileWriter(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private var packageName = ""

    fun createBeansList(beans: List<KSFunctionDeclaration>) {
        logger.warn("Creating beans list")

        if (beans.isNotEmpty()) createPackageName(beans.first())
        val generator = BeansListGenerator(logger, packageName)
        val listContent = generator.generateBeansList(beans)

        val file: OutputStream =
            createFile(beans.toFileList(), "KoGenBeans")
        file += listContent
        file.close()

        val factoryContent = generator.generateBeansFactory(beans)
        val factoryFile: OutputStream =
            createFile(beans.toFileList(), "KoGenBeansFactory")
        factoryFile += factoryContent
        factoryFile.close()
    }

    fun createComponentList(components: List<KSClassDeclaration>) {
        logger.warn("Creating component list")

        if (components.isNotEmpty()) createPackageName(components.first())
        val generator = ComponentListGenerator(logger, packageName)
        val content = generator.generateComponentList(components)

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenComponents")
        file += content
        file.close()

        val factoryContent = generator.createComponentFactory()
        val factoryFile: OutputStream =
            createFile(components.toFileList(), "KoGenComponentsFactory")
        factoryFile += factoryContent
        factoryFile.close()

        createInjectFactory(components)
    }

    private fun createInjectFactory(components: List<KSClassDeclaration>) {
        logger.warn("Creating component factory")

        val generator = InjectFactoryGenerator(packageName)
        val content = generator.generateBeansList()

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenInjectFactory")
        file += content
        file.close()
    }

    private fun createPackageName(component: KSDeclaration) {
        if (packageName.isBlank()) packageName = component.packageName.asString()
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