# Raya Chat UI — ProGuard consumer rules
# These rules are automatically applied to apps that depend on this library.

# Keep all public API entry points
-keep class ai.teammates.rayachat.ui.RayaChatWidget* { *; }
-keep class ai.teammates.rayachat.ui.RayaChatFragment { *; }
-keep class ai.teammates.rayachat.ui.RayaChatBottomSheet { *; }

# Keep adapter interfaces
-keep interface ai.teammates.rayachat.ui.adapters.** { *; }

# Keep Compose classes (R8 can aggressively remove @Composable functions)
-keep class ai.teammates.rayachat.ui.screens.** { *; }
-keep class ai.teammates.rayachat.ui.components.** { *; }
-keep class ai.teammates.rayachat.ui.theme.** { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

# Coil
-keep class coil.** { *; }
