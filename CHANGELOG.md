# Roloam changelog

## 0.6.1-beta — Embedded map containment fix

- Hard-clip OSMDroid's canvas and its Compose container to stop the map bleeding over headers and controls during zoom and pan.
- Capture native map gestures so dragging the map does not move its surrounding page.
- Initialize the map at the actual destination instead of (0, 0), and fit markers after the map's first measured layout.
- Stop route-loading updates from forcibly snapping an already-interacted map viewport.
- Replace the gray checkerboard loading grid with a uniform background while OpenStreetMap tiles arrive.
- Provide an explicit Fit Trip to Map action to recover the viewport without reopening the page.


## 0.6.0-beta — Continue the journey

- Saved accepted trips and their transport/style preferences persist on-device and can be resumed from Home after restarting the app.
- Recent destination history survives restarts; rerolls actively avoid repeating the last destination.
- Routing candidate lookups use smaller batches and handle partial provider outages.
- Return travel is scheduled after the day's final activity, never before it.
- Road/path-following map geometry with graceful fallback; map route loading status.
- Packing checklist and visited-stop progress now persist per trip.
- Regression tests for trip persistence and route parsing.

## 0.5.0-beta — Trip essentials

- More resilient Overpass discovery, including a second provider and small towns for cycling/walking.
- Reroll avoids recently suggested destinations and gives visible feedback.
- Expanded hotel, hostel, cabin and camping searches, with a wider-area fallback.
- Embedded map usability improvements and direct Google Maps directions.
- Accommodation search fallback and direct map listings without claiming live availability.
- Improved transport illustrations; added regression tests.

# Changelog

## 0.4.1-beta

### Home
- Replaces the raw multiline ASCII-text hero with a custom dot-matrix Canvas renderer.
- Draws mountains, pine trees, terrain, campsite detail, and the roaming trail directly so the scene scales cleanly on different phones.
- Keeps dynamic morning, day, evening, and night states.
- Daytime uses a dotted sun; night uses a dotted crescent moon and twinkling stars.
- Removes the floating phase label and whole-scene terminal-text drift.
- Keeps animation subtle: ambient opacity, star twinkle, and a tiny trail movement only.

## 0.4.0-beta

### Dynamic home
- Replaces the static home hero with a live ASCII nature scene system.
- Adds morning, daytime, evening, and night landscape variants.
- Night scenes include moon and stars; daytime scenes use sun-based roaming art.
- Adds subtle breathing and drift animation without turning the home screen into a busy animation.
- Keeps mountains, pine trees, open terrain, and a free-roaming trail as the core visual language.
- Adds a minimal FREE ROAM / NO FIXED ROUTE status line inspired by Nothing-style system UI.
- Removes the static image from the rendered home experience entirely.

## 0.3.3-beta

### Home
- Replaces the procedural mountain renderer with the exact approved hero artwork supplied for the app.
- Preserves the artwork's original 706:519 aspect ratio.
- Removes the repeated visual approximation cycle so the home page now uses the reference composition directly.

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
