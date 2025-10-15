package kz.evko.kogen_di.validation

import com.google.devtools.ksp.symbol.KSDeclaration

data class ProviderNode(
    val concreteType: String,
    val requiredDependencies: List<String>,
    val satisfiableTypes: List<String>,
    val sourceElement: KSDeclaration,
)