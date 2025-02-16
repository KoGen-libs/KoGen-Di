package kz.evko.kogen_di

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import kz.evko.kogen_di.annotations.KoGenBean
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.annotations.KoGenViewModel
import kotlin.reflect.KClass

class KoGenProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val fileWriter = FileWriter(environment.logger, environment.codeGenerator)
        return KoGenProcessor(environment.logger, environment.options, fileWriter)
    }
}

internal class KoGenProcessor(
    private val logger: KSPLogger,
    private val args: Map<String, String>,
    private val fileWriter: FileWriter
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val componentClasses: Sequence<KSClassDeclaration> =
            resolver.findAnnotations(KoGenComponent::class)
                .filterIsInstance<KSClassDeclaration>()
        val beanFunctions: Sequence<KSFunctionDeclaration> =
            resolver.findAnnotations(KoGenBean::class)
                .filterIsInstance<KSFunctionDeclaration>()
        val viewModelClasses: Sequence<KSClassDeclaration> =
            resolver.findAnnotations(KoGenViewModel::class)
                .filterIsInstance<KSClassDeclaration>()

        val packageName = args["packageName"]

        fileWriter.setPackageName(
            packageName,
            listOf(
                *componentClasses.toList().toTypedArray(),
                *beanFunctions.toList().toTypedArray(),
            )
        )
        fileWriter.createInjectFactory()

        fileWriter.createBeansList(beanFunctions.toList())
        fileWriter.createComponentList(componentClasses.toList())

        fileWriter.createViewModelList(viewModelClasses.toList())

        val result: MutableList<KSAnnotated> = mutableListOf()
        result.addAll(componentClasses.filterNot { it.validate() }.toList())
        result.addAll(beanFunctions.filterNot { it.validate() }.toList())
        result.addAll(viewModelClasses.filterNot { it.validate() }.toList())
        return result
    }
}

private fun Resolver.findAnnotations(
    kClass: KClass<*>,
) = getSymbolsWithAnnotation(
    kClass.qualifiedName.toString()
)