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
import kz.evko.kogen_di.annotations.KoGenComponent
import kotlin.reflect.KClass

class KoGenProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val fileWriter = FileWriter(environment.logger, environment.codeGenerator)
        return KoGenProcessor(environment.logger, fileWriter)
    }
}

internal class KoGenProcessor(
    private val logger: KSPLogger,
    private val fileWriter: FileWriter
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val componentClasses: Sequence<KSClassDeclaration> =
            resolver.findAnnotations(KoGenComponent::class)
                .filterIsInstance<KSClassDeclaration>()

        if (!componentClasses.iterator().hasNext()) return emptyList()

        fileWriter.createComponentList(componentClasses.toList())

        fileWriter.createComponentFactory(emptyList())
        return (componentClasses).filterNot { it.validate() }.toList()
    }
}

private fun Resolver.findAnnotations(
    kClass: KClass<*>,
) = getSymbolsWithAnnotation(
    kClass.qualifiedName.toString()
)