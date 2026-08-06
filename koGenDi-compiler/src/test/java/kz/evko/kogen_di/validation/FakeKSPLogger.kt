package kz.evko.kogen_di.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * Минимальный тестовый двойник [KSPLogger] - просто записывает сообщения,
 * ничего не печатает и не бросает исключений на error()/warn(), в отличие
 * от реального KSP-логгера, который валит компиляцию.
 */
class FakeKSPLogger : KSPLogger {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val infos = mutableListOf<String>()

    override fun logging(message: String, symbol: KSNode?) {
        infos += message
    }

    override fun info(message: String, symbol: KSNode?) {
        infos += message
    }

    override fun warn(message: String, symbol: KSNode?) {
        warnings += message
    }

    override fun error(message: String, symbol: KSNode?) {
        errors += message
    }

    override fun exception(e: Throwable) {
        throw e
    }
}
