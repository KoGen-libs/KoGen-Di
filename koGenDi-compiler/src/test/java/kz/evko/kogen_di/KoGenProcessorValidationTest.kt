package kz.evko.kogen_di

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import kz.evko.kogen_di.validation.FakeKSPLogger
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Тест на реальном входе KSP (мокнутом, но той же формы, что даёт resolver) -
 * в отличие от DependencyValidatorTest, здесь проверяется КАК KoGenProcessor
 * строит ProviderNode из символов, а не только сама логика валидатора.
 *
 * Регрессия для бага, ломавшего версии 1.0.5-1.0.7: @KoGenBean не попадал в
 * satisfiableTypes при построении ProviderNode, из-за чего любой класс,
 * зависящий от bean-типа, получал ложную "Missing dependency".
 */
class KoGenProcessorValidationTest {

    private fun qualifiedType(name: String): KSType {
        val declaration = mock(KSDeclaration::class.java)
        val ksName = mock(KSName::class.java)
        `when`(ksName.asString()).thenReturn(name)
        `when`(declaration.qualifiedName).thenReturn(ksName)
        val type = mock(KSType::class.java)
        `when`(type.declaration).thenReturn(declaration)
        return type
    }

    private fun typeReference(name: String): KSTypeReference {
        val resolvedType = qualifiedType(name)
        val reference = mock(KSTypeReference::class.java)
        `when`(reference.resolve()).thenReturn(resolvedType)
        return reference
    }

    private fun bean(functionName: String, returnType: String): KSFunctionDeclaration {
        val returnTypeReference = typeReference(returnType)
        val ksName = mock(KSName::class.java)
        `when`(ksName.asString()).thenReturn(functionName)

        val function = mock(KSFunctionDeclaration::class.java)
        `when`(function.qualifiedName).thenReturn(ksName)
        `when`(function.returnType).thenReturn(returnTypeReference)
        `when`(function.parameters).thenReturn(emptyList())
        return function
    }

    private fun componentRequiring(className: String, requiredType: String): KSClassDeclaration {
        val requiredTypeReference = typeReference(requiredType)
        val ksName = mock(KSName::class.java)
        `when`(ksName.asString()).thenReturn(className)

        val param = mock(KSValueParameter::class.java)
        `when`(param.type).thenReturn(requiredTypeReference)

        val constructor = mock(KSFunctionDeclaration::class.java)
        `when`(constructor.parameters).thenReturn(listOf(param))

        val component = mock(KSClassDeclaration::class.java)
        `when`(component.qualifiedName).thenReturn(ksName)
        `when`(component.superTypes).thenReturn(emptySequence())
        `when`(component.primaryConstructor).thenReturn(constructor)

        return component
    }

    @Test
    fun `component depending on a KoGenBean-provided type does not trigger a false missing dependency`() {
        val logger = FakeKSPLogger()
        val fileWriter = mock(FileWriter::class.java)
        val processor = KoGenProcessor(logger, emptyMap(), fileWriter)

        val beanFunctions = sequenceOf(bean("com.app.provideApiService", "com.app.ApiService"))
        val componentClasses = sequenceOf(
            componentRequiring("com.app.RepositoryImpl", "com.app.ApiService"),
        )

        processor.validateDependencies(
            componentClasses = componentClasses,
            beanFunctions = beanFunctions,
            viewModelClasses = emptySequence(),
        )

        assertTrue("expected no errors but got: ${logger.errors}", logger.errors.isEmpty())
    }

    @Test
    fun `KoGenBean own missing dependency is still caught through the real processor path`() {
        val logger = FakeKSPLogger()
        val fileWriter = mock(FileWriter::class.java)
        val processor = KoGenProcessor(logger, emptyMap(), fileWriter)

        val missingTypeReference = typeReference("com.app.MissingConfig")
        val param = mock(KSValueParameter::class.java)
        `when`(param.type).thenReturn(missingTypeReference)

        val brokenBean = bean("com.app.provideApiService", "com.app.ApiService")
        `when`(brokenBean.parameters).thenReturn(listOf(param))

        processor.validateDependencies(
            componentClasses = emptySequence(),
            beanFunctions = sequenceOf(brokenBean),
            viewModelClasses = emptySequence(),
        )

        assertTrue(logger.errors.any { it.contains("com.app.MissingConfig") })
    }
}
