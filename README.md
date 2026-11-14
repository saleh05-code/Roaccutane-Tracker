<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/c257df3c-d426-412c-8988-2c3896b28d1f

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device


---
## Copyright & Usage Notice
Copyright © 2026 Saleh Mahdi Al Haddad. All Rights Reserved.
This code is shared for portfolio and demonstration purposes only; commercial use is prohibited without explict permission.

​**Disclaimer:** This application is developed for educational and personal portfolio purposes as a prototype. It is not a certified medical device and does not provide medical diagnoses or professional prescriptions. Always consult a qualified healthcare provider in the Kingdom of Saudi Arabia for any medical decisions.
