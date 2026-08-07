package kz.evko.kogen_di.test

import androidx.lifecycle.ViewModel
import kz.evko.kogen_di.annotations.KoGenBean
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.annotations.KoGenViewModel

// --- Service: создаётся через @KoGenBean (функция), синглтон, интерфейс + реализация ---

interface ApiService {
    fun fetchRaw(): String
}

class ApiServiceImpl : ApiService {
    // счётчик создания экземпляров - чтобы в тесте проверить, что singleton реально держит один инстанс
    companion object {
        var instancesCreated = 0
    }

    init {
        instancesCreated++
    }

    override fun fetchRaw(): String = "raw-data"
}

@KoGenBean(singleton = true)
fun provideApiService(): ApiService = ApiServiceImpl()

// --- Repository: @KoGenComponent на реализации, инжектится по интерфейсу, зависит от Service (по интерфейсу) ---

interface DataRepository {
    fun getData(): String
}

@KoGenComponent
class DataRepositoryImpl(
    private val apiService: ApiService,
) : DataRepository {
    override fun getData(): String = "repo(${apiService.fetchRaw()})"
}

// --- UseCase'ы: несколько, каждый - интерфейс + реализация, @KoGenComponent на реализации, зависят от Repository (по интерфейсу) ---

interface GetDataUseCase {
    fun execute(): String
}

@KoGenComponent
class GetDataUseCaseImpl(
    private val repository: DataRepository,
) : GetDataUseCase {
    override fun execute(): String = "get(${repository.getData()})"
}

interface RefreshDataUseCase {
    fun execute(): String
}

@KoGenComponent
class RefreshDataUseCaseImpl(
    private val repository: DataRepository,
) : RefreshDataUseCase {
    override fun execute(): String = "refresh(${repository.getData()})"
}

// --- ViewModel: зависит от обоих UseCase'ов (по интерфейсам) ---

@KoGenViewModel
class ClassicViewModel(
    private val getDataUseCase: GetDataUseCase,
    private val refreshDataUseCase: RefreshDataUseCase,
) : ViewModel() {
    fun combined(): String = "${getDataUseCase.execute()} | ${refreshDataUseCase.execute()}"
}
