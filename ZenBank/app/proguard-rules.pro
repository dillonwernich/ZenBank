# Add project-specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep attributes for debugging stack traces (optional but recommended)
-keepattributes SourceFile, LineNumberTable

# Suppress warnings related to SSL parameters and BouncyCastle library
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

# Firebase ProGuard Rules to avoid stripping necessary classes and constructors

# Keep no-argument constructors for Firebase model classes (e.g., for CustomClassMapper)
-keepclassmembers class * {
    public <init>();
}

# Keep all classes that are used in Firebase Realtime Database serialization/deserialization
-keepclassmembers class com.dillonwernich.zenbank.** {
    *;
}

# Keep Firebase database classes
-keep class com.google.firebase.database.** { *; }

# Keep Firebase authentication classes
-keep class com.google.firebase.auth.** { *; }

# Firebase Crashlytics (optional, if you're using it for crash reporting)
# Keep line numbers and class names for better crash reporting
-keepattributes SourceFile, LineNumberTable
-keepclassmembers class * {
    @com.google.firebase.crashlytics.* <fields>;
}

# Gson serialization/deserialization (if you're using Gson)
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit related rules (if you're using Retrofit for networking)
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Room database protection to prevent stripping of Room-generated classes
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <fields>;
    <methods>;
}

# Keep Kotlin Coroutines from being stripped or obfuscated
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

# Biometric API protection to avoid stripping Biometric API classes
-keep class androidx.biometric.** { *; }

# Keep Google Play services classes (if you're using Google Play services like Google Sign-In)
-keep class com.google.android.gms.** { *; }

# Additional Firebase-related rules
-keep class com.google.firebase.** { *; }
-keepclassmembers class * extends com.google.firebase.** {
    <methods>;
}

# Optional: Keep attributes of methods used in Gson/Retrofit or JSON serialization
-keepattributes *Annotation*
