package kz.evko.kogen_di

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [String.fixOrphanedParameterModifiers] on hand-built sample strings, matching KotlinPoet's actual
 * output shape for a 2-parameter `noinline` function (see the KDoc on that function/the regexes
 * next to it in `FileWriter.kt` for why KotlinPoet produces this in the first place) - a regression
 * guard cheaper than a full KSP compile-test for the exact fix.
 */
class FileWriterFormattingTest {

    @Test
    fun `a modifier orphaned at the end of the previous line is reattached, closing paren on its own line`() {
        val broken = "Fragment.koGenViewModel(noinline\n    extrasProducer: (() -> CreationExtras)? = null)"

        val fixed = broken.fixOrphanedParameterModifiers()

        assertEquals(
            "Fragment.koGenViewModel(\n    noinline extrasProducer: (() -> CreationExtras)? = null\n)",
            fixed,
        )
    }

    @Test
    fun `a real two-parameter noinline signature - one parameter per line, closing paren and return type on their own line`() {
        // exactly the shape KotlinPoet emits for Fragment/ComponentActivity.koGenViewModel(...)
        val broken = "Fragment.koGenViewModel(noinline\n" +
            "    extrasProducer: (() -> CreationExtras)? = null, noinline\n" +
            "    ownerProducer: () -> ViewModelStoreOwner = { this }): ReadOnlyProperty<Fragment, T> ="

        val fixed = broken.fixOrphanedParameterModifiers()

        assertEquals(
            "Fragment.koGenViewModel(\n" +
                "    noinline extrasProducer: (() -> CreationExtras)? = null,\n" +
                "    noinline ownerProducer: () -> ViewModelStoreOwner = { this }\n" +
                "): ReadOnlyProperty<Fragment, T> =",
            fixed,
        )
    }

    @Test
    fun `a param type's own parens don't confuse the scan for the parameter list's real closing paren`() {
        // ownerProducer's own type - `() -> ViewModelStoreOwner` - has a paren pair of its own
        // between the modifier's parameter list and the real closing paren; the fix must still
        // land on the right one.
        val broken = "ComponentActivity.koGenViewModel(noinline extrasProducer: (() -> CreationExtras)? = null, " +
            "noinline ownerProducer: () -> ViewModelStoreOwner = { this }): ReadOnlyProperty<ComponentActivity, T> ="

        val fixed = broken.fixOrphanedParameterModifiers()

        assertEquals(
            "ComponentActivity.koGenViewModel(\n" +
                "    noinline extrasProducer: (() -> CreationExtras)? = null,\n" +
                "    noinline ownerProducer: () -> ViewModelStoreOwner = { this }\n" +
                "): ReadOnlyProperty<ComponentActivity, T> =",
            fixed,
        )
    }

    @Test
    fun `text with no modifiers at all is left untouched`() {
        val text = "public inline fun <reified T> inject(qualifier: String = \"\"): T = koGenModuleId.inject(qualifier)"

        assertEquals(text, text.fixOrphanedParameterModifiers())
    }
}
