# Keep Hilt-generated classes
-keep,allowobfuscation,allowshrinking class * implements dagger.hilt.internal.GeneratedComponent

# Room
-keep class androidx.room.* { *; }
-keep class * extends androidx.room.RoomDatabase

# Jaudiotagger uses reflection on field annotations
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# FFmpegKit native bindings
-keep class com.arthenica.** { *; }
-dontwarn com.arthenica.**

# Chaquopy reflective access
-keep class com.chaquo.python.** { *; }
-keepclassmembers class * { @com.chaquo.python.PyObject *; }
