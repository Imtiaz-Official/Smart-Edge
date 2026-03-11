# Add project specific ProGuard rules here.
# Keep app classes
-keep class com.originpanel.sidepanel.** { *; }
-keepclassmembers class com.originpanel.sidepanel.** { *; }

# Keep ViewBinding
-keep class com.originpanel.sidepanel.databinding.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
