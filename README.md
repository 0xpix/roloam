# Roloam.

**Roll a trip. Go.**

Roloam is a deliberately small Android app for spontaneous 1–3 day escapes. The main interaction is one button: **ROLL A TRIP**.

## Android MVP

- Phone location when permission is granted; Heidelberg is the safe demo fallback.
- Instant random destinations from a bundled offline index near supported German and border cities, without network requests on roll.
- Slot-machine-like character scramble when rolling: symbols change in place and each destination letter locks in sequence, including on rerolls.
- Settings → Typography lets the user choose Doto or Geist Mono for the destination reveal. Both font files are bundled with the APK under their open font licenses; see `THIRD_PARTY_FONTS.md`.
- Live routes, train timetables, mapped attractions, stays and forecast are fetched **only after the user approves the destination**.
- Real road-time scoring for car trips with OSRM.
- Bike and walking route-time support through OpenStreetMap routing endpoints.
- Train trips require a confirmed outbound public-timetable route for the planned date (Transitous MOTIS 2); estimated speeds are never shown as booked or verified trains. Return timetable still needs separate confirmation.
- Real attractions, parks, viewpoints and museums from OpenStreetMap.
- Real campsites, hostels and hotels from OpenStreetMap, with direct website links when available.
- Destination weather from Open-Meteo.
- A compact itinerary engine that balances route distance, place type, weather and useful time-of-day.
- A bounded detail-loading pipeline with cancellable HTTP requests and a 28-second overall timeout for unresponsive public services.
- Minimal screens: Home → Random Destination → Approve → Reveal Details → Plan, with Map / Stay / Place / Now branches.
- Before accepting a trip, Reveal shows day-by-day real mapped stops, expected visit times, suggested campsites/hostels, and Google Maps exact-pin links. A train connection shows actual scheduled outbound time where available, or an error rather than an invented duration.
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

The beta version lives in `BETA_VERSION`. For every beta, bump `BETA_VERSION`, `versionCode`, and the beta version suffix in `app/build.gradle.kts` in normal reviewed commits.

To publish, move the `release/beta` branch to the tested commit. The Beta Release workflow builds `assembleBeta`, creates the matching `vX.Y.Z-beta` tag and GitHub prerelease, and attaches the APK.

Manual version tags such as `v0.2.0-beta` are also supported. Older numbered beta tags remain compatible.

Installed beta builds can then discover later prereleases from Settings.

## Accuracy rules

A random destination is picked without checking routes, attractions, stays or train availability. On approval, Roloam fetches mapped places, accommodation, weather and routes for **that exact destination**. Some sections may be empty when public APIs are unavailable; the app does not substitute another city or invent stops and stays. Route-table metrics are sanity-checked and estimates labeled. OSM listings do not confirm businesses are open or accommodation is bookable. Train durations are displayed only for verified outbound connections from Transitous; return connections must be checked separately.

Roloam does **not** invent campsite prices, availability or opening hours. If live data is unavailable, it says so and falls back only for destination discovery rather than fabricating booking information.

The map shows trip order. It is not intended to replace turn-by-turn navigation; the app hands a selected stop to the installed navigation app.

## Data

- OpenStreetMap + Overpass — destinations, POIs, accommodation
- OSRM / routing.openstreetmap.de — driving, cycling and walking travel times
- Open-Meteo — forecast context

Train routing uses the volunteer-operated [Transitous](https://transitous.org/api/) service, with [source attribution](https://transitous.org/sources/) linked in the app. Its hosted API is for light, noncommercial open-source use; this beta integration is for prototype testing. Before broader distribution, Roloam needs an explicit open-source license and must meet Transitous's usage policy, or use an independently hosted/licensed routing provider.

## Build

Open the repository in Android Studio with JDK 17, or run:

    gradle :app:assembleDebug
    gradle :app:assembleBeta

GitHub Actions runs unit tests and builds both debug and beta APKs.

## Product rule

Keep it small:

**HOME → ROLL → TRIP**

No feed. No travel articles. No endless results.
