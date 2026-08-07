package kz.evko.kogen_di.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KoGenComponent(
    val singleton: Boolean = false,
)