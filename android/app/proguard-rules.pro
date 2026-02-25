# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools directory
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep SDUI models
-keep class com.nba.sdui.core.models.** { *; }

# Keep Jackson annotations
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}

# Keep Ably
-keep class io.ably.** { *; }
