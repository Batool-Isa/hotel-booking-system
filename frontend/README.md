# VibeStay frontend

The web app for VibeStay, built with Vue 3 and Vite. Guests, hotel managers and admins all use this one app. What each person sees depends on their role.

## What is inside

- **Guests:** search hotels, see availability, book rooms, pay with Stripe, manage bookings, write reviews
- **Hotel managers:** create hotels and rooms, upload photos, follow bookings live
- **Admins:** approve hotels, manage users, room types and amenities, see the dashboard
- **Live notifications** over WebSocket (STOMP) with a bell in the header
- **Map view** of hotels with Leaflet and OpenStreetMap
- **360° view:** a WebGL panorama viewer (`PanoViewer.vue`) for equirectangular photos: starts as a "little planet" that unfolds, then drag, pinch or scroll to look around, auto-rotate, full screen. Panoramas live in `public/panoramas/` and are listed in `index.json` (see the README there); the button only shows when a hotel has one. Hotels without their own panorama show the labelled sample
- **Photo viewer:** lightbox with arrows, counter and keyboard keys
- **Small touches:** animated hero, search box that types city names, cards that tilt toward the mouse, shimmering loading placeholders, scroll progress bar, back to top button, an animated tick and confetti when a payment succeeds (all of it respects "reduce motion")

## Tech

Vue 3 (Composition API, `<script setup>`), Vite, Leaflet, `@stomp/stompjs`. No UI framework: the styles are plain CSS in `src/assets`.

## Folder map

| Path | What it does |
|---|---|
| `src/App.vue` | Routing, pages and state |
| `src/components/` | Small reusable pieces (search bar, map, form inputs, booking detail, brand logo) |
| `src/lib/api.js` | Calls the API gateway and handles errors |
| `src/lib/effects.js` | Scroll progress bar, back to top button, confetti |
| `src/lib/utils.js` | Formatting helpers (dates, money, image URLs) |
| `src/assets/main.css`, `wow.css` | Styles and animations |
| `nginx.conf` | Serves the build and forwards `/api`, `/auth`, `/uploads`, `/webhook` and `/ws` to the gateway |
| `Dockerfile` | Builds the app and serves it with nginx |

## Run it

You need Node 22 or newer and the backend running (the API gateway on port 8000).

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. To point it at another backend, set `VITE_API_BASE` (for example `VITE_API_BASE=http://localhost:8000`).

```bash
npm run build      # production build in dist/
npm run preview    # serve the build locally
```

## In Docker

The root `docker-compose.yaml` builds this folder into the `frontend` image and serves it on port 80. nginx sends API calls to the gateway, so the browser only talks to one address.

## Good to know

- Stripe sends the guest back to `/payment/success`, and the app turns that into its own `#/payment/success` route.
- The token is kept in the browser session and sent with every request. WebSocket connections send it in the STOMP `CONNECT` frame.
