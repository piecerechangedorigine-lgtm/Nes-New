# Nova — production ProGuard/R8 rules
#
# Most of what earlier phases might reach for here is already covered by
# consumer rules bundled inside the Hilt, Room and Compose AARs themselves
# — that's what "consumer ProGuard rules" in a library's AAR are for, and
# duplicating them here would just be dead weight R8 has to reconcile.
# What's kept below is only the small set of things those consumer rules
# don't already handle for this specific codebase.

# Hilt's generated entry points are looked up by class name at runtime,
# so they need an explicit keep beyond what Hilt's own consumer rules
# cover for -exact ActivityComponentManager subclasses.
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}

# Room's KSP-generated *_Impl classes reference entity/DAO members
# directly at compile time (no reflection), so they don't generally need
# an explicit keep — but scoping one to this app's own package is cheap
# insurance against a future entity relying on default-constructor
# reflection (e.g. if a raw-query result type is ever added).
-keep class com.novafinance.core.data.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Compose tooling classes are debug/preview-only and legitimately absent
# from a release classpath.
-dontwarn androidx.compose.ui.tooling.**

# Standard recommended keeps for kotlinx.coroutines' internal dispatcher
# service-loading and structured-concurrency exception handling.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Annotations back Hilt's and Room's own runtime/compile-time processing;
# stripping them can break generated code that inspects them.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
