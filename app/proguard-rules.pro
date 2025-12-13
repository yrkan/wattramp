# WattRamp ProGuard rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes
-keep class io.github.wattramp.data.** { *; }

# Keep Karoo extension classes
-keep class io.github.wattramp.WattRampExtension { *; }
-keep class io.github.wattramp.datatypes.** { *; }
