# WattRamp ProGuard rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all classes with @Serializable annotation
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes
-keep class io.github.wattramp.data.** { *; }

# Keep engine classes (TestState, TestEngine)
-keep class io.github.wattramp.engine.** { *; }

# Keep protocol classes
-keep class io.github.wattramp.protocols.** { *; }

# Keep Karoo extension classes
-keep class io.github.wattramp.WattRampExtension { *; }
-keep class io.github.wattramp.datatypes.** { *; }

# Keep Karoo SDK classes
-keep class io.hammerhead.karooext.** { *; }
-dontwarn io.hammerhead.karooext.**

# Keep coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep atomic classes (used for thread safety)
-keep class java.util.concurrent.atomic.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
