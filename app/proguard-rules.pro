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

# Enums resolved by name at runtime (ThemeMode/ExerciseStatus/MealType/DietaryFilter/
# FitnessGoal via valueOf). Keep values()/valueOf so R8 can't rename or strip them.
-keepclassmembers enum com.kalos.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
