# KoGen Di: Руководство пользователя

**KoGen Di** — это библиотека для Android, которая использует кодогенерацию (KSP) для создания простого, независимого и типобезопасного Dependency Injection контейнера, который идеально подходит для многомодульных проектов.

**Основные принципы:**
* **Ноль инициализации:** Не требует привязки к классу `Application`.
* **Независимость:** Каждый модуль может иметь свой собственный, изолированный DI-контейнер.
* **Минимум кода:** Большинство зависимостей регистрируются автоматически с помощью аннотаций.

---

## 🚀 Установка и Настройка

Библиотека опубликована в **Maven Central**.

### Шаг 1: Подключаем плагин KSP

**В файле `build.gradle.kts` корневого проекта:**
```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false // Используйте актуальную версию KSP для вашего Kotlin
}
```

**В файле `build.gradle.kts` вашего модуля:**
```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp")
}
```

### Шаг 2: Добавляем зависимости

Библиотека состоит из двух артефактов: небольшого runtime, на который вы компилируетесь, и KSP-процессора, который работает только во время сборки.
```kotlin
dependencies {
    // Ваша версия может отличаться - смотрите актуальный релиз на Maven Central
    implementation("io.github.eugenprog:android-di:1.1.0")
    ksp("io.github.eugenprog:android-di-compiler:1.1.0")

    // Нужна только если включаете includeViewModelInjector (хелпер @Composable koGenViewModel())
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4")

    // Нужна только если включаете includeFragmentInjector (delegate `by koGenViewModel()` на Fragment/ComponentActivity)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
}
```
**Важно:** *Версии для `implementation` и `ksp` должны совпадать - они всегда публикуются вместе.*

### Шаг 3: Настраиваем кодогенерацию

В `build.gradle.kts` вашего модуля добавьте блок `ksp`.
```kotlin
ksp {
    // Обязательный параметр: пакет вашего проекта
    arg("packageName", "com.myawesome.project")
    // Опциональный параметр для включения поддержки ViewModel в Composable
    arg("includeViewModelInjector", "true")
    // Опциональный параметр для включения delegate `by koGenViewModel()` во Fragment/Activity
    arg("includeFragmentInjector", "true")
}
```
`packageName` — **обязательный** параметр. `includeViewModelInjector` включает `@Composable koGenViewModel()`, `includeFragmentInjector` включает delegate `by koGenViewModel()` во `Fragment`/`ComponentActivity`. Оба опциональны, по умолчанию `false`.

### Шаг 4: Устанавливаем Application Context (если он нужен)

Если у вас есть `@KoGenBean` или `@KoGenComponent`, которым нужен `Context`, вызовите это один раз - обычно в вашем классе `Application`:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setApplicationContext(this)
    }
}
```
Если что-то попытается получить `Context` до того, как это будет вызвано, вы получите `ContextNotFoundException` в runtime. Если ничего из того, что вы получаете через DI, не нуждается в `Context` — этот шаг можно пропустить.

---

## ⚙️ Как пользоваться

### 1. Объявление зависимостей

#### А) Для обычных классов (`@KoGenComponent`)
Пометьте ваш класс (репозиторий, сервис) аннотацией `@KoGenComponent`. Укажите `true`, если он должен быть синглтоном.
```kotlin
@KoGenComponent(singleton = true)
class UserProfileServiceImpl(
    private val source: UserProfileSource,
) : UserProfileService {
    // ...
}
```

#### Б) Для зависимостей со сложным созданием (`@KoGenBean`)
Для объектов, которые требуют сложной логики создания (напр., из `Retrofit`), создайте функцию, которая возвращает этот объект, и пометьте ее аннотацией `@KoGenBean`.
```kotlin
@KoGenBean(singleton = true)
fun provideUserProfileSource(
    context: Context,
): UserProfileSource {
    // ... логика создания объекта
}
```

#### В) Для ViewModel (`@KoGenViewModel`)
Пометьте ваш `ViewModel` соответствующей аннотацией.
```kotlin
@KoGenViewModel
class MyScreenViewModel(
    private val userProfileService: UserProfileService
) : ViewModel() {
    // ...
}
```

### 2. Получение зависимостей

#### Основная точка входа: `inject()`
Это обычная функция, а не property-delegate - присваивайте её через `=`, а не `by`.
```kotlin
class MyActivity : AppCompatActivity() {
    // Получение происходит один раз, при инициализации свойства
    private val userProfileService: UserProfileService = inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val anotherService: AnotherService = inject()
    }
}
```

#### Точки входа для ViewModel

В `@Composable`-экране:
```kotlin
@Composable
fun MyScreen(
    viewModel: MyScreenViewModel = koGenViewModel()
) {
    // ...
}
```

В `Fragment` или `ComponentActivity` (требует `includeFragmentInjector = true`) - как ленивый property-delegate, в отличие от `inject()`, здесь `by` действительно работает:
```kotlin
class MyFragment : Fragment() {
    private val viewModel: MyScreenViewModel by koGenViewModel()
}
```

---

## ✅ Проверка на этапе компиляции

Граф зависимостей проверяется на этапе работы KSP, а не в runtime, когда приложение уже упало у пользователя:

* **Missing dependency** – если классу нужен тип, который никто не предоставляет, сборка падает с ошибкой `Missing dependency: '<тип>' is required by '<класс>' but is not provided.`
* **Ambiguous dependency** – если несколько провайдеров могут предоставить тип, который реально кем-то запрошен, сборка падает с ошибкой `Ambiguous dependency: Type '<тип>' is required, but provided by multiple candidates: ...`

Обе ошибки указывают на конкретный класс, так что их можно исправить до того, как приложение вообще запустится.

---

## ⚠️ Важные замечания

1.  **Код появляется после первой сборки.** Функции `inject()` и `koGenViewModel()` физически отсутствуют в коде до тех пор, пока вы не соберете проект хотя бы один раз. Не пугайтесь, если IDE будет "ругаться" на их отсутствие.

2.  **Поддержка `ViewModel` — опциональна.** Чтобы её активировать, передайте `includeViewModelInjector` (для Composable) и/или `includeFragmentInjector` (для delegate `by koGenViewModel()`) в настройках KSP.

3.  **KSP иногда "сходит с ума".** В редких случаях стандартное лечение — полная очистка проекта (`./gradlew clean`) и пересборка.

[README (English)](README.md)
