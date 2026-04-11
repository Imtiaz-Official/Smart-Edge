# Add project specific ProGuard rules here.
# Keep app classes
-keep class com.imi.smartedge.sidebar.panel.** { *; }
-keepclassmembers class com.imi.smartedge.sidebar.panel.** { *; }

# Keep ViewBinding
-keep class com.imi.smartedge.sidebar.panel.databinding.** { *; }

# Skydoves ColorPickerView
-keep class com.skydoves.colorpickerview.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
