# ai.tour.guide Android app

### Overview
Android app built using following Android Jetpack libraries:
- Compose together with Navigation 3
- DataStore (app state storage)

Other dependencies:
- Koin (Dependency Injection)
- Ktor (Networking)
- Lottie (animations on onboarding screens)

## Development
- Install Android Studio together with all components such as `platform-tools` and `sdk`
- Open the `android/` folder in Android Studio
- Wait for Gradle sync to finish
- Run the app on a device or emulator
    - You will need to turn on USB debugging on your physical device to install the app
    
Development version of the app has `http://localhost:8000` set as the API host address. To connect your device to local instance of the API, you need to run this in your terminal, with the device connected using ADB:

`adb reverse tcp:8000 tcp:8000`

Now the API running on your machine should be routed to localhost on your device.

**Dependencies are managed using Gradle files.**

### High-level overview of app architecture
App was built with my attempt at MVVM and some sort of Clean architecture, as is recommended for Android apps

- Screens are defined as `@Composable` functions. 
- Screens have their own ViewModels, containing business logic and state. 
- ViewModels are injected to screens with DI, data is passed using stateFlows and UI events are called directly on ViewModels.
- ViewModels have their dependencies (API client, repos, etc) injected to the primary constructor with DI


### Main Source Code (`src/main/java/ai/tour/guide/`)
#### `TourGuideApplication.kt`
Extends the `Application` class, used for global initialization.

#### `activity/` Contains the host Activities
Only `MainActivity.kt`, as app is based on the single activity architecture.

#### `config/` App-level configuration and constants.
Static config is defined here, such as API host, Google Auth Client ID, etc.

#### `data/` Core data layer handling business logic and state.
- `appData/` Data handlers for general app state (auth tokens, preferences, etc). App credentials/settings storage is based on Jetpack DataStore.
- `onboardingPreferences/` Manages user preferences and flow during onboarding. Data is fetched from the server and cached inside repo.
- `shared/` Generic logic shared between different features

#### `di/`: Dependency Injection configuration. 
All classes should be autowired by Koin, no changes should be required when introducing new dependencies.

#### `network/`: API client and networking logic using Ktor. 
Contains the API client and request/response schemas.

#### `ui/`: The presentation layer built with Jetpack Compose.
- `components/`: General-purpose reusable UI components and reusable fragments
- `navigation/`: Navigation 3 route definitions and screens config (which screens have drawer navigation, only top bar, etc)
- `screens/`: Contains all the screens for the app, together with associated ViewModels and state definitions, grouped by features.
    - `onboarding/`: Screens for the user registration and configuration flow.
    - `main/`: Everything else
- `theme/`: Styling definitions including color palettes, typography, and shapes.

### Contributing
gl hf