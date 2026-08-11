# KoGen Di: User Guide

**KoGen Di** is a library for Android that uses code generation (KSP) to create a simple, independent, and type-safe Dependency Injection container, perfectly suited for multi-module projects.

**Core Principles:**
* **Zero Initialization:** Does not require binding to the `Application` class.
* **Independence:** Each module can have its own, isolated DI container.
* **Minimal Code:** Most dependencies are registered automatically using annotations.

---

## 🚀 Installation and Setup

The library is published on **Maven Central**. There are two ways to configure it - pick one:

- **Option A - the `koGenDi { }` Gradle plugin** (recommended): typed config, autocomplete,
  checked at Gradle-script-compile time instead of failing silently on a typo'd string. It also
  adds the runtime/compiler dependencies for you.
- **Option B - a raw `ksp { arg(...) }` block**: fewer moving parts, one less plugin to apply.

Both configure the exact same KSP processor underneath - pick whichever fits, or mix per module.

### Step 1: Apply the KSP Plugin

Either way, make sure the KSP plugin is applied to your project first.

**In your root `build.gradle.kts` file:**
```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false // Use the KSP version that matches your Kotlin version
}
```

**In your module's `build.gradle.kts` file:**
```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp")
}
```

### Step 2A: The `koGenDi { }` Plugin (recommended)

```kotlin
plugins {
    // ...
    id("com.google.devtools.ksp")
    id("io.github.eugenprog.kogen-di") version "<version>"
}

koGenDi {
    packageName.set("com.myawesome.project")
    includeViewModelInjector.set(true)
    includeFragmentInjector.set(true)
}

dependencies {
    // Needed only if you enable includeViewModelInjector (the @Composable koGenViewModel() helper)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4")

    // Needed only if you enable includeFragmentInjector (the `by koGenViewModel()` delegate on Fragment/ComponentActivity)
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Nothing else needed here - the plugin adds the KoGen DI runtime and its KSP processor for
    // you, at a matching version.
}
```

The plugin does *not* apply `com.google.devtools.ksp` itself - KSP's own version is tied tightly
to your project's Kotlin version, so you keep controlling that yourself (Step 1 above); the
plugin just requires it to already be applied, and errors clearly if it isn't.

Every property is optional and behaves exactly like its `ksp { arg(...) }` equivalent below - see
Step 2B's list for what each one does; `packageName` left unset falls back to inferring one from
your first `@KoGenComponent`/`@KoGenBean` declaration's own package.

### Step 2B: A Raw `ksp { }` Block (alternative)

The library ships as two artifacts: a small runtime you compile against, and a KSP processor that runs only at build time.
```kotlin
dependencies {
    // Your version may vary - check the latest release on Maven Central
    implementation("io.github.eugenprog:android-di:1.1.0")
    ksp("io.github.eugenprog:android-di-compiler:1.1.0")

    // Needed only if you enable includeViewModelInjector (the @Composable koGenViewModel() helper)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4")

    // Needed only if you enable includeFragmentInjector (the `by koGenViewModel()` delegate on Fragment/ComponentActivity)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
}
```
**Important:** *The `implementation` and `ksp` versions must match - they're always released together.*

```kotlin
ksp {
    // Required parameter: your project's package name
    arg("packageName", "com.myawesome.project")
    // Optional parameter to enable ViewModel support in Composables
    arg("includeViewModelInjector", "true")
    // Optional parameter to enable the `by koGenViewModel()` delegate on Fragment/Activity
    arg("includeFragmentInjector", "true")
}
```
* `packageName` (**required** unless you're fine with the inferred default) – Needed so that the generated classes are placed in the correct namespace of your project.
* `includeViewModelInjector` (optional) – Accepts `true` or `false`. Enables the `@Composable koGenViewModel()` helper. Defaults to `false`.
* `includeFragmentInjector` (optional) – Accepts `true` or `false`. Enables the `by koGenViewModel()` property delegate on `Fragment` and `ComponentActivity`. Defaults to `false`.

### Using a Version Catalog (optional)

Everything above uses plain string literals for clarity. If your project already declares its
plugins/dependencies through a Gradle version catalog (`gradle/libs.versions.toml`) - the
currently recommended way to do it - here's the equivalent:

```toml
[versions]
ksp = "2.1.0-1.0.29" # keep in sync with your Kotlin version - see Step 1 above
kogenDi = "<version>"

[libraries]
kogen-di-runtime = { group = "io.github.eugenprog", name = "android-di", version.ref = "kogenDi" }
kogen-di-compiler = { group = "io.github.eugenprog", name = "android-di-compiler", version.ref = "kogenDi" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kogen-di = { id = "io.github.eugenprog.kogen-di", version.ref = "kogenDi" }
```

```kotlin
// root build.gradle.kts
plugins {
    alias(libs.plugins.ksp) apply false
}
```

```kotlin
// module build.gradle.kts - Option A, the Gradle plugin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kogen.di)
}

// no runtime/compiler dependency entry needed here - the plugin adds them for you, same as with string literals
```

```kotlin
// module build.gradle.kts - Option B, the raw ksp block
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.kogen.di.runtime)
    ksp(libs.kogen.di.compiler)
}
```

### Step 3: Set the Application Context (if you need it)

If any of your `@KoGenBean` or `@KoGenComponent` classes need `Context`, call this once - typically in your `Application` class:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setApplicationContext(this)
    }
}
```
If something tries to inject `Context` before this is called, you'll get a `ContextNotFoundException` at runtime. If nothing you inject needs `Context`, you can skip this step entirely.

---

## ⚙️ How to Use

### 1. Declaring Dependencies

There are two ways to declare a dependency:

#### A) For Regular Classes (`@KoGenComponent`)
Simply annotate your class (e.g., a repository or service implementation) with `@KoGenComponent`. Set `singleton` to `true` if it should be reused for every request.
```kotlin
@KoGenComponent(singleton = true)
class UserProfileServiceImpl(
    private val source: UserProfileSource,
) : UserProfileService {
    // ...
}
```

#### B) For Dependencies with Complex Creation (`@KoGenBean`)
For objects that require complex creation logic (e.g., from `Retrofit` or `Room`), create a function that returns the object and annotate it with `@KoGenBean`.
```kotlin
@KoGenBean(singleton = true)
fun provideUserProfileSource(
    context: Context,
): UserProfileSource {
    // ... object creation logic
}
```

#### C) For ViewModels (`@KoGenViewModel`)
Annotate your `ViewModel` class with the corresponding annotation.
```kotlin
@KoGenViewModel
class MyScreenViewModel(
    private val userProfileService: UserProfileService
) : ViewModel() {
    // ...
}
```

### 2. Retrieving Dependencies

#### Main Entry Point: `inject()`
A plain function - not a property delegate - so assign it directly with `=`, not `by`.
```kotlin
class MyActivity : AppCompatActivity() {
    // Resolved once, when the property is initialized
    private val userProfileService: UserProfileService = inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val anotherService: AnotherService = inject()
    }
}
```

#### ViewModel Entry Points

In a `@Composable` screen:
```kotlin
@Composable
fun MyScreen(
    viewModel: MyScreenViewModel = koGenViewModel()
) {
    // ...
}
```

In a `Fragment` or `ComponentActivity` (requires `includeFragmentInjector = true`), as a lazy property delegate - unlike `inject()`, this one really does support `by`:
```kotlin
class MyFragment : Fragment() {
    private val viewModel: MyScreenViewModel by koGenViewModel()
}
```

---

## ✅ Compile-Time Validation

Your dependency graph is checked while KSP runs, not when the app crashes at runtime:

* **Missing dependency** – if a class asks for a type nobody provides, the build fails with `Missing dependency: '<type>' is required by '<class>' but is not provided.`
* **Ambiguous dependency** – if more than one provider can satisfy a type that's actually requested somewhere, the build fails with `Ambiguous dependency: Type '<type>' is required, but provided by multiple candidates: ...`

Both errors point at the exact class that's missing or conflicting, so you fix them before the app ever runs.

---

## ⚠️ Important Notes

1.  **Code Appears After the First Build.** The functions `inject()` and `koGenViewModel()` are physically absent from the code until you build the project at least once. Don't be alarmed if the IDE complains about their absence.

2.  **ViewModel Support is Optional.** To enable it, pass `includeViewModelInjector` (for Composables) and/or `includeFragmentInjector` (for the `by koGenViewModel()` delegate) in your KSP settings, as shown in the setup guide.

3.  **KSP Can "Go Crazy".** In rare cases, the KSP cache can become corrupted. The standard treatment is a full project clean (`./gradlew clean`) and a rebuild.

[README.ru](README.ru.md)
