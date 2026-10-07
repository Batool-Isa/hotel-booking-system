# VibeStay

## Project Description

VibeStay is a hotel reservation and management platform that allows users to discover hotels, check room availability, make bookings, and manage their reservations.

The system also provides hotel managers with tools to manage their hotels, rooms, and bookings.

## Application Purpose

The purpose of VibeStay is to provide a simple platform for managing hotel reservations while handling room availability, bookings, payments, and user management.

The system will also focus on preventing booking conflicts when multiple users try to reserve the same room.

## Main Features

- User registration and login
- Email verification
- User profiles
- Role-based access control
- Hotel management
- Room management
- Hotel amenities
- Hotel search and filtering
- Room availability
- Hotel room booking
- Booking cancellation
- Booking status management
- Payment and transaction management
- Reviews and ratings
- Notifications
- File upload
- Audit logging
- Real-time updates using WebSockets/SSE
- API documentation using Swagger/OpenAPI
- Pagination and sorting
- Input validation and error handling
- [TODO: AI feature]
- [TODO: Map/location feature]
- [TODO: Additional features]

## Technologies

- Java
- Spring Boot
- Spring Security
- JWT
- PostgreSQL
- Maven/Gradle
- Swagger/OpenAPI
- WebSockets/SSE
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
The project will use DTOs to separate API requests and responses from database entities.

## General Approach
I am using code first appoach in implementing this project.Also this project is implemented and design using MVC structure.
### User Stories
[JIRA Board Link](https://batooltaher124.atlassian.net/jira/software/projects/KAN/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiZjE4NWM5NzU5ZWM1NGExZDk0OTJmODJkMGZiYzc0NmEiLCJwIjoiaiJ9)

### ERD

![ERD.png](ERD.png)
### Planning
Provide a link to your planning documentation/GitHub Project showing:

Deliverables
Timeline
Scope
Progress
API Documentation
Provide access to your Swagger/OpenAPI documentation.

## Installation
Provide clear instructions explaining how another developer can:

Clone the repository.
Configure the application.
Configure PostgreSQL.
Configure environment variables.
Seed the database.
Start the application.
Access the API.
Access Swagger/OpenAPI.
Unsolved Problems
Document any unresolved issues.

## Rescource Used
[Spring Boot - Sending Email via SMTP
](https://www.geeksforgeeks.org/springboot/spring-boot-sending-email-via-smtp/)
https://www.geeksforgeeks.org/springboot/spring-boot-logging/

## Major Challenges
Explain the major technical problems you encountered and how you solved them.

## Future Improvements
Explain what you would add if you had more time.