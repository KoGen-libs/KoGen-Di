# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.8] - 2026-08-07

### Added
- Split the library into three Maven artifacts: `android-di-common` (annotations),
  `android-di` (runtime), `android-di-compiler` (KSP processor). Consumers still add just
  two dependencies (`android-di` + `android-di-compiler`) - `android-di-common` is pulled
  in transitively, invisibly.
- `android-di` is now published as an Android library (AAR) instead of a plain JVM jar, with
  consumer ProGuard/R8 rules (`consumer-rules.pro`) bundled automatically - no manual setup
  needed in the consuming app.
- Compile-time validation reports both missing and ambiguous dependencies, naming the exact
  class and type involved.
- Full test suite: unit tests for the dependency validator and the runtime scope/factories,
  plus real KSP compile tests (`kotlin-compile-testing`) that verify generated code actually
  compiles - including cases that must fail to compile (missing/ambiguous dependency).
- CI runs the full test suite on every pull request to `master`; publishing a new version is
  blocked at the Gradle level if any test fails.

### Changed
- Code generators rewritten with KotlinPoet instead of hand-built strings - generated files
  now have proper imports instead of fully-qualified names on every line.
- `@KoGenComponent` and `@KoGenViewModel` resolution switched from string comparison
  (`Class.simpleName` + package name) to `Class` identity, matching how `@KoGenBean` already
  worked. This removes a class of bugs where R8/ProGuard renaming a class could silently
  break injection at runtime with no compile-time warning.

### Fixed
- **`@KoGenBean` dependencies were invisible to the compile-time validator.** Any class
  depending on a type provided only through `@KoGenBean` was incorrectly rejected with
  `Missing dependency`. This broke 1.0.5 through 1.0.7 for essentially any real project that
  used beans.
- **The ambiguous-dependency check flagged any shared supertype, even when nobody actually
  depended on it.** Two components sharing an unrelated common interface - increasingly
  likely as a codebase grows - triggered a false `Ambiguous dependency` error. This was the
  other reason 1.0.5/1.0.6 broke on real projects; 1.0.7 removed the check entirely instead
  of fixing it, which silently reintroduced the opposite problem (a genuine ambiguity was no
  longer caught at all). It's now fixed properly: ambiguity is only reported for a type that
  is actually requested somewhere.
- **KSP crashed with `FileAlreadyExistsException` on every single build.** The processor
  regenerated its output files on every KSP processing round, including the round(s) KSP
  always runs after generating new files. This was masked by a blanket `try/catch` that
  swallowed the exception at `info` log level (likely the real cause behind occasional "KSP
  going crazy" reports); codegen now runs exactly once, and the `try/catch` is gone.
- Component supertype resolution only checked direct supertypes, not the full inheritance
  chain, which could disagree with the validator (which did check the full chain) and let a
  component pass validation while still not being found at runtime.

### Known issues in previous releases
- **1.0.5, 1.0.6, and 1.0.7 are broken** for most real-world usage and should not be used.
  1.0.4 is the last version before the missing/ambiguous-dependency bugs were introduced.
  These versions cannot be pulled from Maven Central - please upgrade past this release
  instead of pinning to them.

## [1.0.7] - 2025-11-17
### Removed
- Ambiguous-dependency check (see "Fixed" above in Unreleased for the real fix).

## [1.0.6] - 2025-11-16
### Changed
- Validation errors logged at `info` instead of `warn`.
### Removed
- Circular-dependency check.

## [1.0.5] - 2025-10-25
### Added
- Compile-time dependency validation (missing/circular/ambiguous checks).
- `@KoGenViewModel` support for `Fragment`/`Activity` via a property delegate.

## [1.0.4] - 2025-08-01
### Added
- Isolated bean/component factories per module.

## [1.0.3] - 2025-08-01
### Added
- Isolated DI scope per module.

## [1.0.2] - 2025-08-01
### Fixed
- Package names in generated files.

## [1.0.1] - 2025-06-07
### Changed
- Read Maven `groupId` from project properties.

## [1.0.0] - 2025-06-07
- Initial public release.
