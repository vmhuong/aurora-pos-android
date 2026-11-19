# ==============================================================================
# AURORA SYSTEM - PROGUARD / R8 OPTIMIZATION & SHRINKING RULES
# ==============================================================================

# 1. Bảo vệ các thuộc tính Annotation của Firebase Firestore
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.Exclude <methods>;
}

# 2. Giữ nguyên toàn bộ Data Models để Firestore Deserialization (toObject) chạy chính xác 100%
-keep class com.example.qrorderkitchen.models.** { *; }
-keep class com.example.qrorderkitchen.data.models.** { *; }
-keep class com.example.qrorderkitchen.data.model.** { *; }
-keep class com.example.qrorderkitchen.repository.** { *; }

# 3. Bảo vệ Coroutines & Coil Image Loader
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class coil.** { *; }
-dontwarn coil.**

# 4. Bảo vệ Jetpack Compose Runtime
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
