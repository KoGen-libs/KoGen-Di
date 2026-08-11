# KoGen Di: Руководство пользователя

**KoGen Di** — это библиотека для Android, которая использует кодогенерацию (KSP) для создания простого, независимого и типобезопасного Dependency Injection контейнера, который идеально подходит для многомодульных проектов.

**Основные принципы:**
* **Ноль инициализации:** Не требует привязки к классу `Application`.
* **Независимость:** Каждый модуль может иметь свой собственный, изолированный DI-контейнер.
* **Минимум кода:** Большинство зависимостей регистрируются автоматически с помощью аннотаций.

---

## 🚀 Установка и Настройка

Библиотека опубликована в **Maven Central**. Есть два способа её настроить — выберите один:

- **Вариант A — Gradle-плагин `koGenDi { }`** (рекомендуется): typed-конфиг, автокомплит, проверка на этапе компиляции скрипта вместо тихого падения на опечатке в строке. Плюс сам добавляет зависимости на рантайм и компилятор.
- **Вариант B — сырой блок `ksp { arg(...) }`**: меньше подвижных частей, один плагин можно не подключать.

Оба варианта настраивают один и тот же KSP-процессор — выбирайте любой, можно даже разный на разных модулях.

### Шаг 1: Подключаем плагин KSP

В любом случае сначала убедитесь, что плагин KSP подключен к вашему проекту.

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

### Шаг 2A: Плагин `koGenDi { }` (рекомендуется)

```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp")
    id("io.github.eugenprog.kogen-di") version "<версия>"
}

koGenDi {
    packageName.set("com.myawesome.project")
    includeViewModelInjector.set(true)
    includeFragmentInjector.set(true)
}

dependencies {
    // Нужна только если включаете includeViewModelInjector (хелпер @Composable koGenViewModel())
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4")

    // Нужна только если включаете includeFragmentInjector (delegate `by koGenViewModel()` на Fragment/ComponentActivity)
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Больше ничего не нужно - плагин сам добавит рантайм KoGen DI
    // и его KSP-процессор нужной версии.
}
```

Плагин **не** применяет `com.google.devtools.ksp` сам — версия KSP жёстко привязана к версии Kotlin в вашем проекте, поэтому её вы контролируете сами (Шаг 1 выше); плагин только требует, чтобы KSP был уже подключен, и явно об этом сообщает, если это не так.

Каждое поле опционально и работает точно так же, как его аналог из `ksp { arg(...) }` в Шаге 2B — см. список там; `packageName`, если не задан, определяется по пакету первого найденного `@KoGenComponent`/`@KoGenBean`.

### Шаг 2B: Сырой блок `ksp { }` (альтернатива)

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
`packageName` — **обязательный**, если вас не устраивает дефолт по инференсу. `includeViewModelInjector` включает `@Composable koGenViewModel()`, `includeFragmentInjector` включает delegate `by koGenViewModel()` во `Fragment`/`ComponentActivity`. Оба опциональны, по умолчанию `false`.

### Через version catalog (опционально)

Всё выше — с обычными строковыми литералами, для наглядности. Если в вашем проекте плагины/зависимости уже объявляются через Gradle version catalog (`gradle/libs.versions.toml`) — сейчас это рекомендуемый способ — вот эквивалент:

```toml
[versions]
ksp = "2.1.0-1.0.29" # держите синхронно с версией Kotlin - см. Шаг 1 выше
kogenDi = "<версия>"

[libraries]
kogen-di-runtime = { group = "io.github.eugenprog", name = "android-di", version.ref = "kogenDi" }
kogen-di-compiler = { group = "io.github.eugenprog", name = "android-di-compiler", version.ref = "kogenDi" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kogen-di = { id = "io.github.eugenprog.kogen-di", version.ref = "kogenDi" }
```

```kotlin
// build.gradle.kts корневого проекта
plugins {
    alias(libs.plugins.ksp) apply false
}
```

```kotlin
// build.gradle.kts модуля - Вариант A, Gradle-плагин
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kogen.di)
}

// рантайм/компилятор указывать не нужно - плагин добавит их сам, как и со строками
```

```kotlin
// build.gradle.kts модуля - Вариант B, сырой блок ksp
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.kogen.di.runtime)
    ksp(libs.kogen.di.compiler)
}
```

### Шаг 3: Устанавливаем Application Context (если он нужен)

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
