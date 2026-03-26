# Raya Chat Core — ProGuard consumer rules
# These rules are automatically applied to apps that depend on this library.

# Keep all public API classes
-keep class ai.teammates.rayachat.core.RayaChatClient { *; }
-keep class ai.teammates.rayachat.core.RayaChatConfig { *; }

# Keep all model classes (used for serialization)
-keep class ai.teammates.rayachat.core.models.** { *; }

# Keep Room entities and DAO
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep OkHttp WebSocket listener
-keepclassmembers class * implements okhttp3.WebSocketListener { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ai.teammates.rayachat.core.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}
