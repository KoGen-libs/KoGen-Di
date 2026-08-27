package kz.evko.kogen_di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FileSpec
import kz.evko.kogen_di.contentGenerator.BeansListGenerator
import kz.evko.kogen_di.contentGenerator.ComponentListGenerator
import kz.evko.kogen_di.contentGenerator.InjectFactoryGenerator
import kz.evko.kogen_di.contentGenerator.ViewModelListGenerator

/**
 * Owns the package name every generated file is written under, and dispatches to the
 * [BeansListGenerator]/[ComponentListGenerator]/[ViewModelListGenerator]/[InjectFactoryGenerator]
 * content generators, writing whatever `FileSpec` each of them builds.
 */
internal class FileWriter(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private var packageName = ""

    /**
     * Resolves [packageName] once per KSP run: the `packageName` KSP option if set, otherwise the
     * first three segments of the first annotated declaration's own package plus `.di`, falling
     * back to `kz.evko.kogen_di` if there's nothing to infer one from at all.
     */
    fun setPackageName(paramsPackageName: String?, components: List<KSDeclaration>) {
        packageName = paramsPackageName?.plus(".di").takeIf {
            !it.isNullOrEmpty()
        } ?: run {
            val packageParts = components.firstOrNull()?.packageName?.asString()?.split(".")
            packageParts?.subList(0, 3)?.joinToString(".")?.plus(".di") ?: "kz.evko.kogen_di"
        }
    }

    /** Writes `KoGenBeansImpl.kt` and `KoGenBeansFactoryImpl.kt` for every `@KoGenBean` function found. */
    fun createBeansList(beans: List<KSFunctionDeclaration>) {
        logger.info("Creating beans list")

        val generator = BeansListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *beans.toFileList().toTypedArray())

        generator.generateBeansList(beans).writeTo(codeGenerator, dependencies)
        generator.generateBeansFactory(beans).writeTo(codeGenerator, dependencies)
    }

    /** Writes `KoGenComponentsImpl.kt` and `KoGenComponentsFactoryImpl.kt` for every `@KoGenComponent` class found. */
    fun createComponentList(components: List<KSClassDeclaration>) {
        logger.info("Creating component list")
        logger.info("Components count: ${components.size}")

        val generator = ComponentListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *components.toFileList().toTypedArray())

        generator.generateComponentList(components).writeTo(codeGenerator, dependencies)
        generator.createComponentFactory(components).writeTo(codeGenerator, dependencies)
    }

    /** Writes `KoGenViewModelsImpl.kt` and `KoGenViewModelScopeImpl.kt` for every `@KoGenViewModel` class found. */
    fun createViewModelList(viewModels: List<KSClassDeclaration>) {
        logger.info("Creating view model list")
        logger.info("View models count: ${viewModels.size}")

        val generator = ViewModelListGenerator(logger, packageName)
        val dependencies = Dependencies(true, *viewModels.toFileList().toTypedArray())

        generator.generateViewModelList(viewModels).writeTo(codeGenerator, dependencies)
        generator.generateViewModelFactory(viewModels).writeTo(codeGenerator, dependencies)
    }

    /** Writes `KoGenInjectors.kt` - the `inject()`/`setApplicationContext()` entry points, plus the optional Compose/Fragment `koGenViewModel()` ones. Runs once per KSP run, regardless of whether anything is annotated. */
    fun createInjectFactory(includeViewModelInjector: Boolean, includeFragmentInjector: Boolean) {
        logger.info("Creating component factory")

        val generator = InjectFactoryGenerator(packageName)
        generator.generateInjectors(
            includeViewModelInjector = includeViewModelInjector,
            includeFragmentInjector = includeFragmentInjector,
        ).writeTo(codeGenerator, Dependencies(true))
    }
}

/** The distinct source files these declarations came from - what a KSP `Dependencies` needs to track. */
internal fun List<KSDeclaration>.toFileList(): List<KSFile> =
    mapNotNull { it.containingFile }

// KotlinPoet's own parameter-list line-wrapping only forces one-parameter-per-line once a function
// has more than two parameters (see kotlinpoet's ParameterSpec.kt, `List<ParameterSpec>.emit`,
// `emitNewLines = size > 2 || forceNewLines` - not something exposed on FunSpec's public API). With
// exactly one or two parameters (our `noinline extrasProducer`/`noinline ownerProducer` case) it
// falls back to plain greedy word-wrap instead, which doesn't know a parameter modifier
// (`noinline`/`crossinline`/`vararg`) must stay glued to the parameter name right after it, nor
// where the parameter list actually ends - e.g. `Fragment.koGenViewModel(noinline\n
// extrasProducer: ...): ReadOnlyProperty<Fragment, T> =` on one cramped line. This reformatting is
// purely cosmetic (the code is syntactically identical either way) and safe: `noinline`/
// `crossinline`/`vararg` are reserved parameter-modifier keywords that can only ever appear
// directly after `(` or `, ` in valid Kotlin, so matching them there can't misfire on unrelated
// generated text.
private val orphanedParameterModifier = Regex("""\b(noinline|crossinline|vararg)\n\s*""")
private val modifierParameterListStart = Regex("""\((noinline|crossinline|vararg) """)
private val parameterListContinuesOnModifier = Regex(""", (noinline|crossinline|vararg) """)

/** `internal` (rather than `private`) purely so [FileWriterFormattingTest] can exercise it directly on sample strings, without needing a full KSP compilation. */
internal fun String.fixOrphanedParameterModifiers(): String {
    // Reattach a modifier keyword orphaned at the end of a line to the parameter name after it.
    val reattached = replace(orphanedParameterModifier) { "${it.groupValues[1]} " }

    // Lay each parameter of a `noinline`/`crossinline`/`vararg` parameter list on its own line,
    // with the closing `)` (and whatever follows it - the return type, ` =`, ...) on its own line
    // too. This needs an actual paren-depth scan rather than another regex: a parameter's own type
    // can itself contain parens (e.g. `(() -> CreationExtras)?`), so naively matching "the next
    // `)`" would stop at the wrong one instead of the one that actually closes the parameter list.
    val result = StringBuilder()
    var cursor = 0
    while (true) {
        val match = modifierParameterListStart.find(reattached, cursor) ?: break
        val openParen = match.range.first
        result.append(reattached, cursor, openParen + 1)

        var depth = 1
        var scan = openParen + 1
        while (scan < reattached.length && depth > 0) {
            when (reattached[scan]) {
                '(' -> depth++
                ')' -> depth--
            }
            if (depth == 0) break
            scan++
        }
        val closeParen = scan // index of the '(' that matches openParen, or reattached.length if unbalanced

        val parameters = reattached.substring(openParen + 1, closeParen)
            .replace(parameterListContinuesOnModifier) { ",\n    ${it.groupValues[1]} " }
        result.append("\n    ").append(parameters).append("\n)")

        cursor = closeParen + 1
    }
    result.append(reattached, cursor, reattached.length)
    return result.toString()
}

/** Same as KotlinPoet's own `FileSpec.writeTo(codeGenerator, dependencies)`, plus [fixOrphanedParameterModifiers]. */
internal fun FileSpec.writeTo(codeGenerator: CodeGenerator, dependencies: Dependencies) {
    val file = codeGenerator.createNewFile(dependencies, packageName, name)
    file.bufferedWriter(Charsets.UTF_8).use { it.write(toString().fixOrphanedParameterModifiers()) }
}
