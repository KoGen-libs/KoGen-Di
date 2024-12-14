package kz.evko.kogen_di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import kz.evko.kogen_di.contentGenerator.ComponentFactoryGenerator
import kz.evko.kogen_di.contentGenerator.ComponentListGenerator
import java.io.OutputStream

internal class FileWriter(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private var packageName = ""

    fun createComponentList(components: List<KSClassDeclaration>) {
        if (components.isEmpty()) return
        logger.warn("Creating component list")

        createPackageName(components.first())
        val generator = ComponentListGenerator(logger, packageName)
        val content = generator.generateComponentList(components)

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenComponent")
        file += content
        file.close()
    }

    fun createComponentFactory(components: List<KSClassDeclaration>) {
        logger.warn("Creating component factory")

        val generator = ComponentFactoryGenerator(packageName)
        val content = generator.generate()

        val file: OutputStream =
            createFile(components.toFileList(), "KoGenComponentFactory")
        file += content
        file.close()
    }

    private fun createPackageName(component: KSClassDeclaration) {
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

internal fun List<KSClassDeclaration>.toFileList(): List<KSFile> =
    mapNotNull { it.containingFile }