# Infant-Geofencing-and-Alert-System-
Infant Geofencing and Alert System is an IoT-based child safety solution using an ESP32 + NEO-6M GPS tracker that sends real-time location data to a PHP/MySQL backend via WiFi. A native Android app (Kotlin + OpenStreetMap) displays live tracking, lets parents draw a custom geofence, and instantly triggers alerts when the child crosses the boundary.


#  Android App (Kotlin)

A complete native Android app for real-time infant tracking with OpenStreetMap and geofencing.

## 📁 Project Structure

```
app/src/main/
├── kotlin/com/infantgeofence/
│   ├── InfantGeoFenceApp.kt          ← App class, notifications setup
│   ├── data/
│   │   ├── model/Models.kt           ← All data classes
│   │   └── repository/
│   │       ├── ApiService.kt         ← Retrofit API interface
│   │       └── LocationRepository.kt ← Repo + GeofenceEngine (ray-casting)
│   ├── service/
│   │   └── GeofenceMonitorService.kt ← Background foreground service
│   ├── ui/
│   │   ├── MainActivity.kt           ← Host activity + nav
│   │   ├── MainViewModel.kt          ← Shared ViewModel, LiveData, polling
│   │   ├── dashboard/DashboardFragment.kt
│   │   ├── map/MapFragment.kt        ← OSMDroid live map
│   │   ├── geofence/GeofenceFragment.kt ← Draw polygon on map
│   │   └── alerts/AlertsFragment.kt  ← Alert history + RecyclerView
│   └── util/
│       ├── GeofencePrefs.kt          ← Persist polygon in SharedPreferences
│       └── NotificationHelper.kt     ← Push notification helper
└── res/
    ├── layout/                        ← All XML layouts
    ├── navigation/nav_graph.xml       ← Jetpack Navigation graph
    ├── drawable/                      ← 33 vector icons + backgrounds
    ├── menu/bottom_nav_menu.xml
    ├── values/{colors, strings, themes}
    └── mipmap-*/                      ← Launcher icons
```

## ⚙️ Setup Steps

### 1. Open in Android Studio
- File → Open → select this `android_app/` folder
- Let Gradle sync complete

### 2. Configure Server IP
Edit `InfantGeoFenceApp.kt`:
```kotlin
const val BASE_URL = "http://192.168.1.100/infant_geofence/api/"
//                            ↑ your WAMP server IP
```

### 3. Configure Device ID
```kotlin
const val DEVICE_ID = "ESP32_001"   // must match your ESP32 sketch
```

### 4. Build & Run
- Connect Android phone (USB debugging ON) or use emulator
- Click ▶ Run in Android Studio
- Min SDK: Android 7.0 (API 24)

## 🗺️ OpenStreetMap (No API Key!)
Uses **OSMDroid 6.1.18** — tiles from `tile.openstreetmap.org`.
No Google account, no billing, no API key required.

## 📡 Features
| Screen | Features |
|--------|----------|
| **Dashboard** | Online status, geofence status card (green/red), satellites, speed, lat/lng, last update |
| **Live Map** | OSM tiles, child marker (color = fence status), movement trail polyline, follow/pan toggle, fence overlay |
| **Geofence Editor** | Tap-to-draw polygon, numbered points, undo/clear/load/delete, save locally + sync to server |
| **Alerts** | RecyclerView history, unread badge, mark-all-read, swipe to refresh |

## 🔔 Notifications
- **Geofence exit**: High-priority notification with vibration
- **Geofence enter**: Confirmation notification
- **Background service**: Foreground service keeps monitoring alive

## 🔧 Dependencies
```
osmdroid:osmdroid-android:6.1.18     ← OpenStreetMap
retrofit2:retrofit:2.9.0             ← HTTP client
navigation-fragment-ktx:2.7.6        ← Jetpack Navigation
lifecycle-viewmodel-ktx:2.7.0        ← ViewModel/LiveData
work-runtime-ktx:2.9.0               ← Background work
material:1.11.0                      ← Material Design 3
```

## 🔗 Backend
Uses the same PHP backend from the full project:
- `send_location.php` ← ESP32 pushes GPS here
- `get_location.php`  ← App polls here every 5s
- `set_geofence.php`  ← App saves fence polygon
- `get_alerts.php`    ← App reads alert history
