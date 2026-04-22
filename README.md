# HireHub

HireHub is a modern Android application designed to empower job seekers and career enthusiasts. It leverages AI-driven roadmaps, job tracking, and career management tools to streamline the path to professional success.

## 🚀 Features

- **AI-Powered Career Roadmaps**: Generate personalized learning paths and skill roadmaps using Google Gemini AI.
- **Job Discovery & Management**: Explore career opportunities and keep track of your applications.
- **Resume Processing**: Extract and analyze text from PDF resumes using specialized libraries.
- **Real-time Synchronization**: Powered by Firebase for seamless authentication and data storage.
- **Modern UI/UX**: Includes Shimmer loading effects, Lottie animations, and a responsive design using Material Components.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Components**: ViewBinding, Material Design, Shimmer (Facebook), Lottie (Airbnb)
- **Networking**: Retrofit, OkHttp, Gson
- **Backend/Cloud**: Firebase Auth, Firestore, Realtime Database, Cloud Storage
- **AI Integration**: Google Generative AI (Gemini SDK)
- **Utilities**: Coroutines, Lifecycle (ViewModel, LiveData), Glide (Image Loading), PDFBox (PDF Processing)

## 📋 Prerequisites

- Android Studio Koala or newer
- JDK 11
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 36
- A Google Gemini API Key

## ⚙️ Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/HireHub.git
   ```

2. **Configure Firebase**:
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.hirehuborg.careers`.
   - Download the `google-services.json` file and place it in the `app/` directory.

3. **API Keys**:
   - Open your `local.properties` file in the root directory.
   - Add your Gemini API key:
     ```properties
     GEMINI_API_KEY=your_api_key_here
     ```

4. **Build and Run**:
   - Sync the project with Gradle files.
   - Run the app on an emulator or a physical device.

## 📁 Project Structure

```text
com.hirehuborg.careers
├── data        # Data models and repositories
├── domain      # Business logic and use cases
├── network     # Retrofit interfaces and API clients
├── ui          # Activities, Fragments, ViewModels, and Adapters
│   └── activities
├── utils       # Helper classes and extensions
└── HireHubApplication.kt
```

