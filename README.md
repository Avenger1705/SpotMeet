# SpotMeet 📍

SpotMeet is an Android application designed to help users discover, share, and meet up at various locations. It leverages a modern Android tech stack with real-time location and map features, paired with a lightweight Python backend for file and email handling.

## 📱 Android Client Features

The Android app is built using traditional Android Views (XML layouts) and integrates several powerful libraries:

- **Maps & Location:** Uses `osmdroid` for rich, interactive OpenStreetMap integration, and Google's `play-services-location` for precise user tracking.
- **Backend as a Service (BaaS):** Powered by **Firebase Firestore** for real-time NoSQL database management, storing user profiles, spots, and meetups.
- **Image Loading:** Uses **Glide** for fast, efficient loading of images, alongside `androidx.exifinterface` for handling photo metadata.
- **UI & Navigation:** Built with Material Design components, `ConstraintLayout`, and `AppCompat`.

### Building the App

1. Open the project in Android Studio.
2. Add your `google-services.json` file (from your Firebase Console) into the `app/` directory.
3. Build and run the app on an emulator or physical device running API 24 (Android 7.0) or higher.

## 🖥️ Python Server (Backend)

While Firebase handles the main database, SpotMeet utilizes a lightweight **Flask** server for specialized tasks that are better handled off-device:

- **Email Service:** Automatically sends transactional or notification emails.
- **Image Processing:** Uploads images and generates optimized thumbnails.

### Server Setup & Execution

All server files are kept cleanly in the `server/` directory to separate them from the Android application code.

1. **Navigate to the server directory:**
   ```bash
   cd server
   ```

2. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Configuration:**
   - Copy `.env.example` to `.env`
   - Fill in your `SENDER_EMAIL` and `APP_PASSWORD`.
   > *Note: If you're using a Gmail account, you need to create an "App Password" in your Google Account security settings.*

4. **Run the server:**
   ```bash
   python main.py
   ```
   The server runs on port `5001`.

### Server Endpoints

- `GET /`: Health check.
- `POST /send-email`: Send an email. Expects JSON: `{"to": "email@example.com", "subject": "Hello", "message": "World"}`.
- `POST /upload-image`: Upload an image field named `image`. Returns the image URL and a thumbnail URL.
- `GET /files/<filename>`: Serve the uploaded files.

## 🛠️ Tech Stack Overview

| Area | Technologies |
|---|---|
| **Android** | Kotlin/Java, XML, Material Design |
| **Mapping** | OSMDroid (OpenStreetMap), Google Play Location |
| **Database** | Firebase Firestore |
| **Images** | Glide |
| **Backend** | Python 3, Flask, Pillow (Image Processing) |

## 🤝 Contributing
Feel free to open issues or submit pull requests. Ensure any new dependencies are added properly to the `app/build.gradle.kts` or `server/requirements.txt` respectively.
