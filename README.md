# Roloam.

**Roll a trip. Go.**

Roloam is a deliberately small Android app for spontaneous 1–3 day escapes. The main interaction is one button: **ROLL A TRIP**.

## Android MVP

- Phone location when permission is granted; Heidelberg is the safe demo fallback.
- Nearby destinations from OpenStreetMap / Overpass.
- Real road-time scoring for car trips with OSRM.
- Bike and walking route-time support through OpenStreetMap routing endpoints.
- Train mode is usable, but its travel time is currently an estimate; exact timetable routing is the next accuracy milestone.
- Real attractions, parks, viewpoints and museums from OpenStreetMap.
- Real campsites, hostels and hotels from OpenStreetMap, with direct website links when available.
- Destination weather from Open-Meteo.
- A compact itinerary engine that balances route distance, place type, weather and useful time-of-day.
- Minimal screens: Home → Roll → Reveal → Plan, with Map / Stay / Place / Now branches.
- Animated journey header changes between car, train, bike and walking.
- No API keys required.

## Accuracy rules

Roloam does **not** invent campsite prices, availability or opening hours. If live data is unavailable, it says so and falls back only for destination discovery rather than fabricating booking information.

The map shows trip order. It is not intended to replace turn-by-turn navigation; the app hands a selected stop to the installed navigation app.

## Data

- OpenStreetMap + Overpass — destinations, POIs, accommodation
- OSRM / routing.openstreetmap.de — driving, cycling and walking travel times
- Open-Meteo — forecast context

## Build

Open the repository in Android Studio with JDK 17, or run:

    gradle :app:assembleDebug

GitHub Actions runs unit tests, builds the debug APK, and uploads it as the **roloam-debug-apk** artifact.

## Product rule

Keep it small:

**HOME → ROLL → TRIP**

No feed. No travel articles. No endless results.
