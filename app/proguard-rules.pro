# ── Preserve line numbers for crash debugging ────────────────────────────────
-keepattributes SourceFile,LineNumberTable

# ── Firebase Realtime Database ────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.hirehuborg.careers.data.model.** {
    *;
}

# ── Gemini SDK ────────────────────────────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }

# ── Keep all your data/model classes ─────────────────────────────────────────
-keep class com.hirehuborg.careers.data.model.** { *; }
-keep class com.hirehuborg.careers.data.repository.** { *; }

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Suppress existing warning ─────────────────────────────────────────────────
-dontwarn com.gemalto.jp2.JP2Decoder

# ── Firebase KTX (fixes Missing class error) ──────────────────────────────────
-dontwarn com.google.firebase.ktx.**
-keep class com.google.firebase.ktx.** { *; }
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.appcheck.** { *; }