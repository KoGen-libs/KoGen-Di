package kz.evko.kogen_di.test

import kz.evko.kogen.di.KoGenViewModelScopeImpl
import kz.evko.kogen.di.inject
import kz.evko.kogen_di.viewModel.KoGenViewModelScope
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * End-to-end проверка классической слоистой структуры:
 * @KoGenBean(singleton) Service -> @KoGenComponent Repository (по интерфейсу) ->
 * несколько @KoGenComponent UseCase'ов (по интерфейсу) -> @KoGenViewModel.
 *
 * Идёт через реально сгенерированный KSP-код, а не мок - если граф зависимостей
 * собран неверно, здесь либо не скомпилируется, либо бросит ComponentNotFoundException.
 */
class ClassicStructureTest {

    @Test
    fun `use cases resolve end-to-end through the repository and the bean-backed service`() {
        val getData = inject<GetDataUseCase>()
        val refreshData = inject<RefreshDataUseCase>()

        assertEquals("get(repo(raw-data))", getData.execute())
        assertEquals("refresh(repo(raw-data))", refreshData.execute())
    }

    @Test
    fun `bean-backed service is a true singleton across independent injection chains`() {
        val before = ApiServiceImpl.instancesCreated

        // два независимых пути к одному и тому же @KoGenBean(singleton = true)
        inject<GetDataUseCase>()
        inject<RefreshDataUseCase>()
        inject<ApiService>()

        assertEquals(
            "singleton service must not be re-created across separate resolutions",
            before,
            ApiServiceImpl.instancesCreated,
        )
    }

    @Test
    fun `view model receives both use cases through the generated scope`() {
        val scope = KoGenViewModelScope.getInstance(
            scopeId = "kz.evko.kogen.di",
            reference = KoGenViewModelScopeImpl::class.java,
        )

        val viewModel = scope.getViewModel(ClassicViewModel::class.java)

        assertEquals(
            "get(repo(raw-data)) | refresh(repo(raw-data))",
            viewModel?.combined(),
        )
    }
}
