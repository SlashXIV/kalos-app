-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.kalos.app.**$$serializer { *; }
-keepclassmembers class com.kalos.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.kalos.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-dontwarn com.google.dagger.**
-keep class dagger.hilt.** { *; }
