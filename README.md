# VibeStay

VibeStay is a hotel booking platform. Guests search for hotels, check which rooms are free on their dates, and book one or more rooms. Hotel managers run their own hotels, and admins approve hotels and look after users.

I built it as a real booking application, not just a set of CRUD endpoints. Most of the work went into the booking flow: room availability, guest ages, capacity limits, roles and live notifications.

## What it does

**Guests**
- Register, verify their email, log in, reset or change a password
- Search hotels by city and dates, filter by price and room type
- See hotels on a map (list and map view)
- Book several rooms in one booking, with the name and age of every guest
- Cancel a booking and see their own bookings
- Get a booking email with the hotel address, a map and directions

**Hotel managers**
- Create hotels (an admin approves them) and manage only their own hotels
- Manage rooms, room status, photos, amenities and the child policy
- See new bookings live, without refreshing the page

**Admins**
- Approve, reject, activate or deactivate hotels
- Assign managers to hotels
- Manage users (activate, deactivate) and room types

**Behind the scenes**
- JWT login, role based access, password hashing, input validation
- Rate limit on login, audit log, one error format for every API error
- Pagination, sorting, search and filtering
- Swagger documentation
- Real time notifications with WebSocket and STOMP

## Built with

Java 17, Spring Boot, Spring Security, JWT, PostgreSQL, Maven, Swagger/OpenAPI, WebSocket/STOMP, a Vue front end, Leaflet with OpenStreetMap, Git and GitHub.

The code follows a layered structure: Controller, Service, Repository, Database. DTOs keep the API separate from the database entities, and the database is created from the entities (code first).

## Planning

I planned the project in Jira before writing code, then built it in small steps.

[Jira board](https://batooltaher124.atlassian.net/jira/software/projects/KAN/boards/1)

| Phase | What I did |
|---|---|
| 1. Scope | Decided the roles, the main features and what to leave for later |
| 2. Design | Wrote the user stories and drew the ERD |
| 3. Foundation | Users, login, JWT, roles, validation and error handling |
| 4. Hotels and rooms | Hotels, approval, rooms, room types, amenities, images, child policy |
| 5. Booking | Availability search, multi room booking, guests, cancellation |
| 6. Extras | Emails, WebSocket notifications, audit log, rate limit |
| 7. Front end and testing | Vue front end, map, demo data and a smoke test |
| 8. Final polish | Fixes, documentation and the demo |

User stories, tasks and progress were tracked on the Jira board.

## ERD

The first design was drawn in Lucidchart and then updated to match the final entities.

[View on Lucidchart](https://lucid.app/lucidchart/8b0e637d-298f-47b5-a125-42f609a4f752/edit)

![ERD](ERD.png)

The editable source is `docs/erd.mmd` (Mermaid).

## Getting started

**1. Clone**

```bash
git clone https://github.com/Batool-Isa/hotel-booking-system.git
cd hotel-booking-system
```

**2. Create a PostgreSQL database** and note its name and login.

**3. Set the environment variables.** No secrets are stored in the repository (see `.env.example`).

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (default), `test` or `prod` |
| `DATABASE_URL` | for example `jdbc:postgresql://localhost:5432/hotel-booking-app` |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL login |
| `JWT_SECRET` | Base64 key, at least 32 bytes (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_MS` | token lifetime, default 86400000 (24 hours) |
| `EMAIL_USERNAME`, `EMAIL_PASSWORD` | SMTP account (a Gmail app password) |

The `test` profile uses an in-memory H2 database and needs no secrets.

**4. Run**

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8000`. Swagger is at `http://localhost:8000/swagger-ui/index.html`.

**5. Demo data.** The first time the app starts on an empty database, it adds demo data. If the database already has any users or hotels, it adds nothing. For a fresh start, drop and recreate the database.

| Demo data | Details |
|---|---|
| Admin | `admin@vibestay.com` / `Admin123!` |
| Managers | `manager1` to `manager6@vibestay.com` / `Manager123!` |
| Customers | `customer1` to `customer16@vibestay.com` / `Customer123!` |
| Hotels | 11 hotels in Bahrain, UAE, Qatar, Saudi Arabia, Oman and Kuwait (8 active, 2 pending, 1 inactive) |
| Rooms | 11 rooms per hotel, across 6 room types |
| Bookings | 26 sample bookings (upcoming, completed and cancelled) |

These passwords are fake and for development only.

## API documentation

Base URL: `http://localhost:8000`. The full interactive documentation is in Swagger at `http://localhost:8000/swagger-ui/index.html`, where you can log in, authorize with a token and try every endpoint.

"Public" means no token is needed. Everything else needs the header `Authorization: Bearer <token>`. Lists accept `page`, `size` and `sort`, for example `?page=0&size=10&sort=name,asc`.

### Authentication and profile (`/auth/users`)

| Method | Path | Access | What it does |
|---|---|---|---|
| POST | `/auth/users` | Public | Register (profile photo is optional) |
| GET | `/auth/users/verify-email?token=` | Public | Verify email |
| POST | `/auth/users/login` | Public | Log in, returns the JWT |
| POST | `/auth/users/forgot-password` | Public | Send a password reset link |
| POST | `/auth/users/reset-link` | Public | Set a new password with the link token |
| POST | `/auth/users/change-password` | Logged in | Change my password |
| GET, PUT | `/auth/users/profile` | Logged in | View or update my profile |
| PUT | `/auth/users` | Logged in | Change my profile photo |

### Hotels (`/api/hotels`)

| Method | Path | Access | What it does |
|---|---|---|---|
| GET | `/api/hotels` | Public | Search live hotels (`search`, `name`, `city`, `country`) |
| GET | `/api/hotels/{id}` | Public | Hotel details with images, amenities and policy |
| POST | `/api/hotels` | Admin, Manager | Create a hotel (a manager's hotel waits for approval) |
| PUT | `/api/hotels/{id}` | Admin, Manager | Update a hotel (managers: only their own) |
| GET | `/api/hotels/my-hotels` | Manager | Hotels I manage |
| GET | `/api/hotels/pending` | Admin | Hotels waiting for approval |
| PATCH | `/api/hotels/{id}/status` | Admin | Approve, reject, activate or deactivate |
| POST | `/api/hotels/{hotelId}/managers/{managerId}` | Admin | Assign a manager to a hotel |
| GET, POST, DELETE | `/api/hotels/{hotelId}/images[/{imageId}]` | Public to read, Admin and Manager to change | List, upload and delete hotel photos |
| GET | `/api/hotels/{hotelId}/child-policy` | Public | View the child policy |
| POST, PUT | `/api/hotels/{hotelId}/child-policy` | Admin, Manager | Create or update the child policy |

### Rooms and room types

| Method | Path | Access | What it does |
|---|---|---|---|
| POST | `/api/hotels/{hotelId}/rooms` | Admin, Manager | Add a room |
| POST | `/api/hotels/{hotelId}/rooms/bulk` | Admin, Manager | Add up to 100 rooms at once (all or nothing) |
| GET | `/api/hotels/{hotelId}/rooms` | Admin, Manager | List rooms |
| GET, PUT | `/api/hotels/{hotelId}/rooms/{roomId}` | Admin, Manager | View or update one room |
| PATCH | `/api/hotels/{hotelId}/rooms/{roomId}/status` | Admin, Manager | ACTIVE, INACTIVE or UNDER_MAINTENANCE (blocked if the room has upcoming bookings) |
| GET | `/api/hotels/{hotelId}/rooms/{roomId}/images` | Public | Room photos |
| POST, DELETE | `/api/hotels/{hotelId}/rooms/{roomId}/images[/{imageId}]` | Admin, Manager | Upload or delete room photos |
| GET | `/api/rooms-types`, `/api/rooms-types/{id}` | Admin, Manager | View room types |
| POST, PUT, PATCH | `/api/rooms-types[/{id}[/status]]` | Admin | Create, update, activate or deactivate room types |

### Amenities

| Method | Path | Access | What it does |
|---|---|---|---|
| GET | `/api/amenities` | Public | The amenity catalog |
| POST | `/api/amenities` | Admin, Manager | Add an amenity to the catalog |
| PUT, DELETE | `/api/amenities/{id}` | Admin | Rename or delete (not allowed while a hotel uses it) |
| POST | `/api/hotels/{hotelId}/amenities` | Admin, Manager | Add an amenity to a hotel (description, extra cost) |
| PUT, DELETE | `/api/hotels/{hotelId}/amenities/{amenityId}` | Admin, Manager | Change or remove it |

### Availability and bookings

| Method | Path | Access | What it does |
|---|---|---|---|
| POST | `/api/availability/search` | Public | Find hotels and rooms free for the dates and guests |
| POST | `/api/bookings` | Logged in | Create a booking (one or more rooms, guests with ages) |
| GET | `/api/bookings/my-bookings` | Customer | My bookings |
| GET | `/api/bookings/{bookingId}` | Customer, Manager, Admin | Booking details (own booking, own hotel, or any for admin) |
| PATCH | `/api/bookings/{bookingId}/cancel` | Customer, Manager, Admin | Cancel a confirmed booking |
| PATCH | `/api/bookings/{bookingId}/status` | Manager, Admin | Mark a booking COMPLETED |

### Admin

| Method | Path | Access | What it does |
|---|---|---|---|
| GET | `/api/admin/users` | Admin | Users with filters `status`, `role`, `search`, paging and sorting |
| GET | `/api/admin/users/{id}` | Admin | One user |
| PATCH | `/api/admin/users/{id}/status` | Admin | Activate or deactivate a user |
| DELETE | `/api/admin/users/{id}` | Admin | Deactivate a user (soft delete, data is kept) |

### Example: make a booking

First find free rooms:

```http
POST /api/availability/search
{ "checkIn": "2026-11-10", "checkOut": "2026-11-13", "adults": 2, "children": 1, "city": "Manama" }
```

Then book them (needs a customer token):

```http
POST /api/bookings
{
  "hotelId": 1,
  "checkIn": "2026-11-10",
  "checkOut": "2026-11-13",
  "specialRequest": "Late check-in please",
  "rooms": [
    { "roomId": 5,
      "guests": [ { "name": "Sara Ahmed", "age": 32 }, { "name": "Omar Ahmed", "age": 6 } ] }
  ]
}
```

The reply contains the booking reference, and a confirmation email is sent.

### Errors

Every error uses the same JSON shape, with an `errors` map for validation problems:

```json
{ "timestamp": "2026-10-08T05:48:09", "status": 409, "error": "ALREADY_EXISTS", "message": "User with email ... already exists!", "path": "/auth/users", "errors": null }
```

| Status | Meaning |
|---|---|
| 400 | Validation failed or a business rule was broken (for example a room that is already booked) |
| 401 | Missing or invalid token |
| 403 | Logged in, but not allowed to do this |
| 404 | Not found |
| 409 | Already exists (for example the same email or room number) |
| 429 | Too many login attempts, wait a minute |

## Real time notifications

VibeStay uses WebSocket with STOMP at `ws://localhost:8000/ws`.

A browser cannot send an `Authorization` header when it opens a WebSocket, so the JWT goes in the STOMP `CONNECT` frame. The server checks the token and that the account is active, and refuses the connection otherwise. Every subscription is checked too, and clients cannot publish.

| Destination | Who can subscribe | What they receive |
|---|---|---|
| `/topic/users/{your email}/bookings` | any logged in user | their own booking events |
| `/topic/hotels/{hotelId}/bookings` | managers of that hotel, and admins | events for that hotel |
| `/topic/admin/bookings` | admins | every booking event |

```json
{ "bookingReference": "VB1A2B", "message": "Booking confirmed successfully", "status": "CONFIRMED", "hotelId": 1, "hotelName": "VibeStay Manama Grand" }
```

Events are sent when a booking is confirmed or cancelled.

### Using it

**In the app.** Log in and a bell icon appears in the header. When a booking is made or cancelled, a pop-up message shows, the bell collects it in a live list, and the My bookings page refreshes without a page reload. Customers are told about their own bookings, managers about their hotels, and admins about everything.

**From the command line.** `docs/ws_listen.py` connects with a login, subscribes, and prints every event it receives. It needs Python 3 and `pip install websockets`.

```
py docs/ws_listen.py EMAIL PASSWORD DESTINATION
```

```
py docs/ws_listen.py customer1@vibestay.com Customer123! /topic/users/customer1@vibestay.com/bookings
py docs/ws_listen.py manager1@vibestay.com Manager123! /topic/hotels/1/bookings
py docs/ws_listen.py admin@vibestay.com Admin123! /topic/admin/bookings
```

Leave the window open, then make or cancel a booking in the app. The event shows up in the window straight away. Subscribing to a destination you are not allowed to use, such as another customer's topic or a hotel you do not manage, returns an `ERROR` frame.

**Any STOMP client.** Open `ws://localhost:8000/ws`, send a `CONNECT` frame with the header `Authorization: Bearer <token>`, then `SUBSCRIBE` to a destination above. Every STOMP frame ends with a NUL character, which is why a plain text WebSocket tool such as Postman is hard to use here.

## Emails

The app sends verification, password reset and booking emails. They share one design. The booking email shows the reference, hotel, dates, guests, rooms, nights and total, plus a map of the hotel and a "Get directions" button. The map picture is built on the server from OpenStreetMap tiles, so no API key is needed. If the map cannot be made, the email is still sent without it.

## Main challenges

**Room availability.** Before a booking is created, the app checks for an overlapping booking on the same room. A room is taken when `existing check-in < requested check-out` and `existing check-out > requested check-in`. A new guest can still check in on the day another guest leaves.

**Bookings with many rooms and guests.** One booking can hold several rooms, and each room holds its own guests (`Booking`, `BookingRoom`, `BookingGuest`). Guest information stays attached to the room they sleep in.

**Guest ages.** Each hotel has a child policy that sets the ages for infants, children and adults, and whether infants count towards room occupancy.

**Roles.** Customers manage their own bookings, managers manage the hotels assigned to them, and admins manage the system. A `STAFF` role exists for future use.

**Secure WebSocket.** At first every booking event went to one shared topic that anyone could join. I changed it so the token is checked when the socket connects, guests get a private queue, and managers can only listen to their own hotels.

**Emails with a map.** Google's static map needs billing, so I replaced it with a map picture built from OpenStreetMap tiles.

## Unsolved problems

- Two people booking the same room at the exact same moment need stronger locking.
- Booking emails are sent while the booking is being created, so a slow mail server can slow the request down.

## Resources

- [Spring Boot: sending email via SMTP](https://www.geeksforgeeks.org/springboot/spring-boot-sending-email-via-smtp/)
- [Spring Boot logging](https://www.geeksforgeeks.org/springboot/spring-boot-logging/)

## Future improvements

- Online payments
- Reviews and ratings
- AI hotel recommendations or a booking assistant
- Search by location on the map
- Analytics and a fuller admin dashboard
- Sending emails in the background
- A mobile app
