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

## Beta channel

The beta app is intentionally isolated from a future Play Store production app:

- Beta package: `com.roloam.app.beta`
- Production package: `com.roloam.app`
- Beta updater exists only when `BuildConfig.BETA_CHANNEL == true`
- Settings → GitHub beta updates checks GitHub prereleases from `0xpix/roloam`
- Downloaded APKs are verified to match the current package and signing certificate before Android opens the installer
- Android may ask the user once to allow Roloam Beta to install unknown apps

The beta signing key committed in this repository is deliberately a **throwaway beta-only key**. It must never be reused for a production build. Keeping the beta package ID separate means the public beta key cannot sign or replace a future production installation.

### Publish a beta

Beta releases are created from tags matching:

    v*-beta.*

Example:

    git tag v0.1.0-beta.1
    git push origin v0.1.0-beta.1

GitHub Actions builds `assembleBeta`, creates a GitHub prerelease, and attaches the APK. Installed beta builds can then discover later prereleases from Settings.

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
    gradle :app:assembleBeta

GitHub Actions runs unit tests and builds both debug and beta APKs.

## Product rule

Keep it small:

**HOME → ROLL → TRIP**

No feed. No travel articles. No endless results.
