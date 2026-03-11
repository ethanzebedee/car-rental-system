# 🚗 Car Rental System

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.0-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/github/actions/workflow/status/ethanzebedee/car-rental-system/ci.yml)](https://github.com/ethanzebedee/car-rental-system/actions)

A car rental system built with Java 21 and Spring Boot 4. Provides a RESTful API for managing car reservations and fleet inventory, with full OpenAPI documentation, Docker support, and a CI/CD pipeline.

## ✨ Features

- **Fleet Management**: Manage different car types (Sedan, SUV, Van) with configurable inventory
- **Reservation System**: Book cars for specific dates and durations with automatic availability checking and overlap detection
- **RESTful API**: Complete REST API with OpenAPI/Swagger documentation
- **Database Integration**: H2 in-memory database with JPA/Hibernate persistence
- **Comprehensive Testing**: 29 unit tests with Mockito, covering business logic, overlap detection, validation, and error cases
- **Input Validation**: Validated inputs with structured JSON error responses
- **Docker Support**: Multi-stage Dockerfile for optimised container builds
- **CI/CD**: GitHub Actions pipeline that builds, tests, and validates the Docker image on every push

## 🏗️ Architecture

Layered architecture with clear separation of concerns:

- **Domain Layer**: Core business entities (`Car`, `Reservation`, `CarType`)
- **Service Layer**: Business logic, validation, and transaction management
- **Controller Layer**: REST API endpoints with OpenAPI annotations
- **Repository Layer**: Data access via Spring Data JPA

### Key Design Decisions

- **Immutable Reservations**: Once created, reservations cannot be modified
- **Overlap Detection**: `Reservation.overlaps()` uses half-open interval logic — adjacent bookings are permitted, overlapping ones are rejected
- **Custom Exceptions**: `NoAvailableCarException` maps to 409 Conflict; `IllegalArgumentException` maps to 400 Bad Request
- **Spring Profiles**: `dev` profile is the default and enables the H2 console and verbose SQL logging; configure a separate profile for production deployments

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker (optional)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/ethanzebedee/car-rental-system.git
   cd car-rental-system
   ```

2. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

   The application starts on `http://localhost:8080` with the `dev` profile active. To run with a specific profile:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```

3. **Run tests**

   ```bash
   mvn test
   ```

### Docker

```bash
# Build the Docker image (multi-stage build)
docker build -t car-rental-system .

# Run with Docker
docker run -p 8080:8080 car-rental-system
```

## 📖 API Documentation

Once the application is running, visit:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/v3/api-docs
- **H2 Console** (dev profile): http://localhost:8080/h2-console

### Endpoints

| Method | Endpoint                         | Description              | Success |
| ------ | -------------------------------- | ------------------------ | ------- |
| POST   | `/api/reservations`              | Create a new reservation | 201     |
| GET    | `/api/reservations`              | List all reservations    | 200     |
| GET    | `/api/reservations/{id}`         | Get reservation by ID    | 200     |
| DELETE | `/api/reservations/{id}`         | Cancel a reservation     | 204     |
| GET    | `/api/reservations/availability` | Check car availability   | 200     |

### Example Requests

```bash
# Check availability
curl "http://localhost:8080/api/reservations/availability?type=SUV&startDate=2026-07-01T10:00:00&days=3"

# Create a reservation
curl -X POST "http://localhost:8080/api/reservations?carType=SEDAN&startDate=2026-07-01T10:00:00&days=5"

# Cancel a reservation
curl -X DELETE http://localhost:8080/api/reservations/{id}
```

### Error Responses

All errors return a structured JSON body:

```json
{
  "code": "NO_CAR_AVAILABLE",
  "message": "No SEDAN cars available from 2026-07-01T10:00 for 5 days",
  "timestamp": 1234567890
}
```

| HTTP Status | Code               | Cause                               |
| ----------- | ------------------ | ----------------------------------- |
| 400         | `INVALID_REQUEST`  | Null/invalid input parameters       |
| 404         | —                  | Reservation ID not found            |
| 409         | `NO_CAR_AVAILABLE` | No car free for the requested dates |
| 500         | `INTERNAL_ERROR`   | Unexpected server error             |

## 🧪 Testing

```bash
mvn test
```

The test suite uses Mockito to mock repository dependencies, so tests run against the real production code path. Coverage includes:

- ✅ Core reservation and cancellation logic
- ✅ Inventory limit enforcement
- ✅ Overlap detection (partial, complete, adjacent, boundary cases)
- ✅ Input validation (null parameters, zero/negative days)
- ✅ Availability counting with pre-existing reservations
- ✅ All retrieval and count methods

## 🛠️ Project Structure

```
src/
├── main/
│   ├── java/com/example/carrental/
│   │   ├── Application.java
│   │   ├── config/
│   │   │   └── OpenApiConfig.java
│   │   ├── controller/
│   │   │   └── ReservationController.java
│   │   ├── domain/
│   │   │   ├── Car.java
│   │   │   ├── CarType.java
│   │   │   └── Reservation.java
│   │   ├── exception/
│   │   │   ├── ErrorResponse.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── NoAvailableCarException.java
│   │   ├── repository/
│   │   │   ├── CarRepository.java
│   │   │   └── ReservationRepository.java
│   │   └── service/
│   │       └── CarRentalService.java
│   └── resources/
│       ├── application.properties          # Base (production-safe) config
│       └── application-dev.properties      # Dev overrides (H2 console, SQL logging)
└── test/
    └── java/com/example/carrental/
        └── service/
            └── CarRentalServiceTest.java
```

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request against `main`

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

**Made by Ethan Hammond**
