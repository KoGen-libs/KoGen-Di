package kz.evko.kogen_di

import kz.evko.kogen_di.validation.DependencyValidator
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
import kz.evko.kogen_di.validation.ProviderNode
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
    private val fileWriter: FileWriter,
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

        validateDependencies(
            componentClasses = componentClasses,
            beanFunctions = beanFunctions,
            viewModelClasses = viewModelClasses,
        )

        fileWriter.setPackageName(
            args["packageName"],
            listOf(
                *componentClasses.toList().toTypedArray(),
                *beanFunctions.toList().toTypedArray(),
            )
        )
        fileWriter.createInjectFactory(
            includeViewModelInjector = args["includeViewModelInjector"] == "true",
            includeFragmentInjector = args["includeFragmentInjector"] == "true",
        )

        if (!componentClasses.iterator().hasNext() &&
            !beanFunctions.iterator().hasNext() &&
            !viewModelClasses.iterator().hasNext()
        ) return emptyList()

        fileWriter.createBeansList(beanFunctions.toList())
        fileWriter.createComponentList(componentClasses.toList())

        fileWriter.createViewModelList(viewModelClasses.toList())

        val result: MutableList<KSAnnotated> = mutableListOf()
        result.addAll(componentClasses.filterNot { it.validate() }.toList())
        result.addAll(beanFunctions.filterNot { it.validate() }.toList())
        result.addAll(viewModelClasses.filterNot { it.validate() }.toList())
        return result
    }

    fun validateDependencies(
        componentClasses: Sequence<KSClassDeclaration>,
        beanFunctions: Sequence<KSFunctionDeclaration>,
        viewModelClasses: Sequence<KSClassDeclaration>,
    ) {
        val allProviders = mutableListOf<ProviderNode>()

        componentClasses.forEach { classDeclaration ->
            val primaryConstructor = classDeclaration.primaryConstructor
            if (primaryConstructor == null) {
                logger.error(
                    "@KoGenComponent class must have a primary constructor",
                    classDeclaration
                )
                return@forEach
            }
            val concreteType = classDeclaration.qualifiedName?.asString() ?: ""
            val allSatisfiableTypes = listOf(concreteType) + classDeclaration.getSuperTypes()
            classDeclaration.qualifiedName?.let { name ->
                allProviders.add(
                    ProviderNode(
                        concreteType = concreteType,
                        requiredDependencies = primaryConstructor.parameters.map { param ->
                            param.type.resolve().declaration.qualifiedName?.asString() ?: ""
                        },
                        satisfiableTypes = allSatisfiableTypes,
                        sourceElement = classDeclaration,
                    )
                )
            }

        }

        beanFunctions.forEach { functionDeclaration ->
            val returnType = functionDeclaration.returnType?.resolve()
            if (returnType == null) {
                logger.error("@KoGenBean function must have a return type", functionDeclaration)
                return@forEach
            }
            val concreteType = functionDeclaration.qualifiedName?.asString() ?: ""
            returnType.declaration.qualifiedName?.let { name ->
                allProviders.add(
                    ProviderNode(
                        concreteType = concreteType,
                        requiredDependencies = functionDeclaration.parameters.map { param ->
                            param.type.resolve().declaration.qualifiedName?.asString() ?: ""
                        },
                        satisfiableTypes = listOf(name.asString()),
                        sourceElement = functionDeclaration,
                    )
                )
            }
        }

        viewModelClasses.forEach { classDeclaration ->
            val primaryConstructor = classDeclaration.primaryConstructor
            if (primaryConstructor == null) {
                logger.error(
                    "@KoGenViewModel class must have a primary constructor",
                    classDeclaration
                )
                return@forEach
            }
            val concreteType = classDeclaration.qualifiedName?.asString() ?: ""
            val allSatisfiableTypes = listOf(concreteType) + classDeclaration.getSuperTypes()
            classDeclaration.qualifiedName?.let { name ->
                allProviders.add(
                    ProviderNode(
                        concreteType = concreteType,
                        requiredDependencies = primaryConstructor.parameters.map { param ->
                            param.type.resolve().declaration.qualifiedName?.asString() ?: ""
                        },
                        satisfiableTypes = allSatisfiableTypes,
                        sourceElement = classDeclaration,
                    )
                )
            }
        }

        DependencyValidator(allProviders, logger).validate()
    }
}

private fun KSClassDeclaration.getSuperTypes(): List<String> {
    val superTypes = mutableSetOf<String>()
    this.superTypes.forEach { typeReference ->
        val resolvedType = typeReference.resolve()
        val declaration = resolvedType.declaration as? KSClassDeclaration ?: return@forEach
        declaration.qualifiedName?.let {
            val name = it.asString()
            if (name != "kotlin.Any") {
                superTypes.add(name)
            }
        }
        superTypes.addAll(declaration.getSuperTypes())
    }
    return superTypes.toList()
}

private fun Resolver.findAnnotations(
    kClass: KClass<*>,
) = getSymbolsWithAnnotation(
    kClass.qualifiedName.toString()
)