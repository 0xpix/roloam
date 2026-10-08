# Changelog

## 0.3.2-beta

### Home
- Reworked the home hero illustration to better match the approved concept board.
- Enlarged the scene and made the winding trail the visual anchor.
- Added stronger layered depth with a dominant left mountain, long right ridge, denser foreground pines, and a clearer valley opening.
- Reduced symmetry and made the campsite a subtle one-sided detail.
- Tightened the intro → artwork → Roll a Trip spacing so the home screen reads as one composition.

## 0.3.1-beta

### Home
- Rebuilt the hero illustration again to more closely match the approved concept board.
- Uses square pixel/stipple marks rather than smooth abstract dots.
- Adds layered mountain silhouettes, denser pine clusters, valley texture, a winding trail, campsite detail, and a centered warm ring sun.
- Removes the large weighted spacer below the artwork so the scene and Roll a Trip button read as one composition.

## 0.3.0-beta

### Home
- Rebuilt the hero illustration again to match the approved concept more closely: dense stippled mountain layers, pine clusters, valley texture, winding path, campsite detail, and warm ring sun.
- Removed the sparse abstract look from the previous beta.

### Weather
- Added a dedicated Weather screen for the rolled trip.
- Fetches hourly temperature, precipitation probability, wind and weather code from Open-Meteo.
- Calculates a practical best morning departure window from rain, wind and cold penalties.
- Shows compact hourly forecast points for each trip day.
- Falls back safely when hourly forecast data is unavailable.

### Packing
- Added a real trip-specific packing checklist.
- Adapts to trip duration, camping preference, transport mode and forecast.
- Adds rain gear, warm layers or sunscreen only when the weather supports it.
- Adds bike, train, walk or car-specific items.
- Tracks checklist progress locally while the screen is open.

### Plan
- Added Weather and Pack actions directly to the trip plan.
- Kept Map, Stay and Start easy to reach without adding a permanent navigation bar.

### Beta pipeline
- BETA_VERSION is now the source used by CI for beta APK versioning.
- CI verifies the APK's installed version name before a beta can be published.

## 0.2.0-beta

### Branding
- Replaced the temporary mark with the approved dice + dotted route + two-point Roloam identity.
- Rebuilt the launcher icon around the same mark.
- Added a custom pixel-style in-app wordmark.
- Added consistent branded headers across trip screens.

### Home
- Replaced the abstract illustration with a dotted/pixel travel landscape inspired by the approved concept.
- Added mountains, trees, a winding route, a small campsite glyph, and a warm accent sun.
- Polished the primary Roll a Trip button and bottom controls.

### Trip experience
- Kept the smarter destination scoring introduced in the previous beta.
- Kept opening-hours-aware itinerary generation.
- Kept route-aware campsite ranking.
- Added Today / Stay / Full trip map views.
- Refined preferences styling for light and dark mode.

### Beta channel
- Renamed the release to the simpler version scheme: 0.2.0-beta.
- Kept compatibility with older numbered beta names such as 0.1.0-beta.2.
