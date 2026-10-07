# VibeStay

## Project Description

VibeStay is a hotel reservation and management platform that allows users to discover hotels, check room availability, make bookings, and manage their reservations.

The system also gives hotel managers and administrators tools to manage hotels, rooms, and bookings.

I built VibeStay as a practical hotel booking application rather than just a collection of CRUD endpoints. The main focus is on the booking flow, room availability, user roles, validation, and the business rules around making and managing reservations.

## Application Purpose

The purpose of VibeStay is to provide a simple platform for managing hotel reservations while handling room availability, bookings, hotel management, and user management.

The application also focuses on preventing invalid bookings, such as booking a room that is already reserved for overlapping dates or exceeding the room's allowed capacity.

## Main Features

### User and Authentication
- User registration and login
- JWT authentication
- Email verification
- Forgot password
- Reset password
- Change password
- User profiles
- Profile picture upload
- Role-based access control
- Soft delete and inactive user handling

### Hotel Management
- Hotel creation and management
- Hotel manager assignment
- Hotel approval workflow
- Hotel status management
- Hotel images
- Hotel amenities
- Child policies
- Hotel search and filtering
- Pagination and sorting
- Hotel location using latitude and longitude

### Room Management
- Room management
- Room types
- Room capacity management
- Adult and child occupancy limits
- Room availability
- Multiple rooms in one booking
- Room status management

### Booking Management
- Hotel room booking
- Multiple rooms in one booking
- Guest information
- Guest age classification
- Booking reference generation
- Booking availability validation
- Booking capacity validation
- Booking total calculation
- Booking cancellation
- Booking status management
- View booking details
- View my bookings

### Notifications and Email
- Booking confirmation emails
- Booking status email notifications
- HTML booking emails
- Real-time booking notifications using WebSockets/STOMP
- Hotel location included in booking emails

### Security and API
- Spring Security
- JWT authentication
- Role-based authorization
- Password hashing
- Input validation
- Global exception handling
- Rate limiting for sensitive authentication endpoints
- Audit logging
- Swagger/OpenAPI documentation
- Pagination and sorting
- Search and filtering
- DTO-based API requests and responses

## Technologies

- Java
- Spring Boot
- Spring Security
- JWT
- PostgreSQL
- Maven
- Swagger/OpenAPI
- WebSockets
- STOMP
- Git/GitHub

## Architecture

The application follows a layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

I am using a code-first approach for implementing the application and database entities.

The project follows an MVC structure and uses DTOs to separate API requests and responses from database entities.

## User Stories

Project planning and user stories were managed using Jira.

[Jira Board](https://batooltaher124.atlassian.net/jira/software/projects/KAN/boards/1)

## ERD

The database structure was designed using Lucidchart.

[View ERD on Lucidchart](https://lucid.app/lucidchart/8b0e637d-298f-47b5-a125-42f609a4f752/edit)

![ERD](ERD.png)

## Planning

The project was planned and tracked using Jira.

The planning included:
- Deliverables
- Timeline
- Scope
- Progress
- User stories
- Development tasks

## API Documentation

Swagger/OpenAPI is used to document and test the REST API.

When running the application locally, Swagger UI is available at:

```text
http://localhost:8000/swagger-ui/index.html
```

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/Batool-Isa/hotel-booking-system.git
cd hotel-booking-system
```

### 2. Configure PostgreSQL

Create a PostgreSQL database for the application and configure the database connection.

### 3. Configure Environment Variables

Sensitive configuration is stored using environment variables instead of committing credentials to the repository.

The application uses environment variables such as:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
EMAIL_USERNAME
EMAIL_PASSWORD
GOOGLE_MAPS_API_KEY
```

### 4. Seed the Database

The application includes seed data for required initial data such as:
- Roles
- Admin user
- Hotel managers
- Amenities

### 5. Start the Application

The application can be started using IntelliJ or Maven:

```bash
./mvnw spring-boot:run
```

### 6. Access the API

The application runs locally on:

```text
http://localhost:8000
```

### 7. Access Swagger

```text
http://localhost:8000/swagger-ui/index.html
```

## WebSocket

VibeStay uses WebSockets with STOMP for real-time booking notifications.

The WebSocket endpoint is:

```text
ws://localhost:8000/ws
```

Booking events such as confirmation and cancellation can trigger real-time notifications.

A small browser-based STOMP client was used to test and demonstrate the WebSocket functionality.

## Email Notifications

VibeStay sends booking-related email notifications.

The booking email includes:
- Booking reference
- Hotel information
- Check-in and check-out dates
- Guest information
- Room information
- Booking status
- Number of nights
- Total amount
- Hotel location
- Google Maps location

The email is generated as HTML to make the booking information easier to read.

## Major Challenges

### Booking Availability

One of the main challenges was handling room availability.

Before creating a booking, the application checks whether the selected room already has an overlapping booking.

The overlap rule is:

```text
existing check-in < requested check-out
AND
existing check-out > requested check-in
```

This prevents conflicting reservations while still allowing a new guest to check in on the same day another guest checks out.

### Booking and Guest Relationships

A booking can contain multiple rooms, and each booked room can contain multiple guests.

The relationship is structured as:

```text
User
  ↓
Booking
  ↓
BookingRoom
  ↓
Room

BookingRoom
  ↓
BookingGuest
```

This allowed me to keep guest information connected to the specific room they are staying in.

### Guest Age Classification

Guests are classified based on the hotel's child policy.

The system handles:
- Adults
- Children
- Infants

The policy also determines whether infants count towards room occupancy.

### Role-Based Authorization

VibeStay has different roles with different permissions:
- CUSTOMER
- HOTEL_MANAGER
- ADMIN

For example, customers can manage their own bookings, hotel managers can manage hotels assigned to them, and administrators can perform system-level management actions.

### Real-Time Notifications

WebSocket/STOMP was one of the features I had to spend time debugging and testing.

The application sends a real-time notification when booking events occur, such as a booking being confirmed or cancelled.

I also created a small browser-based test client to demonstrate that the notification can be received without refreshing the page.

### Email and HTML Formatting

Another challenge was building a structured HTML email containing booking information.

The email dynamically displays booking details, hotel information, rooms, guests, total price, and the hotel's location.

The Google Maps Static API was also integrated into the email so the hotel location can be displayed as a map.

## Unsolved Problems

- Google Maps Static API requires billing to be enabled on the Google Cloud project before the map image can be displayed.
- WebSocket notifications currently use a shared topic and can be improved to send notifications only to the intended user.
- More advanced concurrency handling for simultaneous booking attempts can be added in a future iteration.

## Resources Used

- [Spring Boot - Sending Email via SMTP](https://www.geeksforgeeks.org/springboot/spring-boot-sending-email-via-smtp/)
- [Spring Boot Logging](https://www.geeksforgeeks.org/springboot/spring-boot-logging/)

## Future Improvements

If I had more time, I would like to extend VibeStay with:
- A frontend application for customers and hotel managers
- Online payment integration
- AI-powered hotel recommendations or an AI booking assistant
- Improved Google Maps and location-based hotel search
- More advanced booking concurrency handling
- User-specific real-time notifications
- Reviews and ratings
- Hotel and booking analytics
- A more complete admin dashboard
- Advanced hotel and room search
- Mobile application support

## Project Status

VibeStay is currently being developed as a Spring Boot hotel reservation and management application.

The core authentication, hotel, room, booking, notification, email, and real-time functionality has been implemented. Additional improvements and frontend features can be added as the project continues.
