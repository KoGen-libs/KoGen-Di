package kz.evko.kogen_di.test

import kz.evko.kogen_di.NameService
import kz.evko.kogen_di.annotations.KoGenComponent

interface NameUseCase {
    fun getName(): String
}

@KoGenComponent
class NameUseCaseImpl(private val service: NameService) : NameUseCase {
    override fun getName(): String = service.getName()
}