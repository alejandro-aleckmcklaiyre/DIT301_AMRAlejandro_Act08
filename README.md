# SimpleLocationTracker - Real-Time GPS Location App

A simple Android app that displays your real-time location on an interactive OpenStreetMap.

---

## 📱 App Description

SimpleLocationTracker is a straightforward GPS tracking app that shows your current location on a map in real-time. When you open the app, it asks for location permission, then displays an interactive OpenStreetMap centered on your position with a marker showing exactly where you are. As you move around, the marker updates automatically to follow you. The app uses OpenStreetMap through the Osmdroid library, which is completely free and doesn't require any API keys, making it perfect for learning and development. You can zoom in and out, pan around the map, and see your precise latitude and longitude coordinates displayed on the marker.

---

## 🔐 Permissions Used

The app requires two location permissions to function:

**ACCESS_FINE_LOCATION** - This is the main permission that allows the app to access precise GPS location from your device's GPS sensor. It provides accurate location data down to a few meters, which is necessary for showing your exact position on the map.

**ACCESS_COARSE_LOCATION** - This is a backup permission that allows the app to get approximate location using cell towers and Wi-Fi networks if GPS isn't available. It's less accurate (usually within a few hundred meters) but works indoors and in areas where GPS signal is weak.

The app asks for these permissions when it first launches using the modern ActivityResultContracts API. If you deny the permission, the app still works but shows a default location (San Francisco) instead of your actual position. You can always grant permission later by going to your phone's settings.

---

## 📍 How GPS Location Is Obtained

The app uses Google's FusedLocationProviderClient, which is a smart location service that combines data from GPS, Wi-Fi, and cell towers to give you the most accurate location possible. Here's how it works: when the app starts, it first checks if you've granted location permission. If yes, it immediately tries to get your last known location (which is super fast because Android caches it) and shows that on the map right away. Then, it sets up continuous location updates that come in every second using a LocationCallback. Each time your location changes, the callback receives the new coordinates and updates the marker on the map to match your new position. The app also moves the map camera to keep you centered, so you're always in view. When you minimize the app or your screen turns off, it automatically stops requesting location updates to save battery, then resumes when you come back. The whole process is seamless and gives you real-time tracking without draining your battery unnecessarily.

---

## 🛠️ Technologies Used

- Kotlin
- Osmdroid (OpenStreetMap - no API key needed!)
- FusedLocationProviderClient
- LocationCallback for real-time updates
- View Binding

---

## 🚀 Getting Started

1. Clone the repository
2. Open in Android Studio
3. Run on a physical device (GPS works best on real devices)
4. Grant location permission when prompted
5. Watch your location update in real-time!

**Note:** The app works best on a physical device. Emulators require manual location simulation.

---

**Created as a GPS location tracking learning project.**
