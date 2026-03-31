import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
}

/*
 Read API key from local.properties
 */
val geminiApiKey: String = gradleLocalProperties(rootDir, providers)
    .getProperty("GEMINI_API_KEY") ?: ""


android {
    namespace = "com.hirehuborg.careers"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    aaptOptions {
        noCompress("pdf")
    }

    defaultConfig {
        applicationId = "com.hirehuborg.careers"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"$geminiApiKey\""
        )
    }

    buildTypes {

        debug {
            buildConfigField(
                "String",
                "GEMINI_API_KEY",
                "\"$geminiApiKey\""
            )
        }

        release {
            isMinifyEnabled = true

            buildConfigField(
                "String",
                "GEMINI_API_KEY",
                "\"$geminiApiKey\""
            )

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.credentials)
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.firebase.database)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.5.0")

    // PDF Text Extraction
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Already in your build.gradle.kts — just confirm
//    implementation("com.squareup.retrofit2:retrofit:2.11.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
//    implementation("com.google.code.gson:gson:2.11.0")

// Add this new one for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.cardview:cardview:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}