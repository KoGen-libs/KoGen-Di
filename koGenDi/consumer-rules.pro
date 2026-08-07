# KoGen DI consumer rules.
#
# This file lives under META-INF/proguard/ inside the published jar - R8/AGP
# discover and apply it automatically for any consumer, Android library or
# not. No wiring needed in the consuming app's own proguard-rules.pro.

# Per-module generated factories/scopes (KoGenBeansFactoryImpl,
# KoGenComponentsFactoryImpl, KoGenViewModelScopeImpl) are instantiated
# reflectively via Class.getConstructor().newInstance() in KoGenScope /
# KoGenViewModelScope - there is no static call to their constructor, so R8
# has no reachability edge to it and will strip/inline it as dead code.
-keep class * extends kz.evko.kogen_di.injector.KoGenBeansFactory {
    <init>();
}
-keep class * extends kz.evko.kogen_di.injector.KoGenComponentsFactory {
    <init>();
}
-keep class * extends kz.evko.kogen_di.viewModel.KoGenViewModelScope {
    <init>();
}

# @KoGenComponent / @KoGenBean / @KoGenViewModel are all resolved at runtime
# by Class identity (Map<Class<*>, ...>), not by comparing name strings, so
# none of them need name-keeping - safe under renaming/obfuscation as-is.
