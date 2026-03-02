# Car Rental System

**Technical Assessment – Ethan Hammond**  
Charles River Development | March 2026

Java 21 • Maven • JUnit 5 • 32 Tests Passing

---

Hi, I'm Ethan. This document walks you through the Car Rental System I built for this assessment. You can follow along as I present, or use it as a reference afterward.

## The Brief

The task was straightforward: **design and implement a simulated car rental system using object-oriented principles.**

Requirements:

- Reserve a car by type (**Sedan**, **SUV**, or **Van**) at a given date and time for N days.
- Enforce **limited fleet sizes** per type, no overbooking.
- **Prove it works** with comprehensive unit tests.

I chose Java 21 with Maven because it aligns with the job description's tech stack and is a modern LTS release that maps directly to the requirements.

## Domain Model – the Core Design

I split the problem into five classes, each with a single responsibility:

| Class                       | Purpose                                                    |
| --------------------------- | ---------------------------------------------------------- |
| **CarType**                 | Enum: `SEDAN`, `SUV`, `VAN`                                |
| **Car**                     | A physical vehicle with a unique ID and type               |
| **Reservation**             | Immutable record of a booking: car, start time, duration   |
| **NoAvailableCarException** | Custom exception for failed bookings                       |
| **CarRentalService**        | Entry point: manages fleet inventory and reservation logic |

The key design choice was **making Reservation immutable**. Once you book a car, the reservation object can't change. This eliminates an entire class of bugs as no one can alter a reservation mid-check

**CarRentalService** holds two pieces of state:

- A map of inventory limits per car type (`Map<CarType, Integer>`)
- A list of active reservations (`CopyOnWriteArrayList`)

To reserve a car, the service filters the fleet by type, checks each candidate for overlapping reservations, and either returns a booking or throws `NoAvailableCarException` with a descriptive message.

## Implementation – Key Decisions

### Availability & Overlap Detection

The core algorithm is straightforward. When you request a car for a date range, the service checks all existing reservations for that car type. If any overlap with your requested dates, that car is out.

Overlap condition:

```
existingStart < requestEnd  &&  existingEnd > requestStart
```

This handles partial overlaps, full overlaps, and allows back-to-back bookings. A car returned on Day 5 at midnight is available from Day 5 at midnight.

### Thread Safety

The reservation list uses `CopyOnWriteArrayList`. Reads are lock-free; writes create a new copy of the backing array. This is ideal for a read-heavy scenario where you check availability far more often than you create bookings.

### Input Validation – Fail Fast

Every public method validates its inputs at the entry point:

- Null car type? Rejected.
- Null start time? Rejected.
- Zero or negative days? Rejected.

Clear error messages make bugs obvious and keep the service state clean.

### Main API

```java
// Reserve a car (throws if unavailable)
Reservation reserveCar(CarType type, LocalDateTime start, int days)

// Check how many cars are free for a given period
int getAvailableCarsCount(CarType type, LocalDateTime start, int days)

// Cancel a reservation and return the car to the pool
boolean cancelReservation(String reservationId)

// View all reservations in the system
List<Reservation> getAllReservations()
```

## SOLID Principles – Architectural Thinking

This was designed with the SOLID principles in mind. Here's how each applies:

### Single Responsibility Principle (SRP)

Each class has one reason to change:

- **CarType** – changes only if business adds/removes car categories
- **Car** – changes only if we modify how a physical vehicle is represented
- **Reservation** – changes only if booking data structure evolves
- **NoAvailableCarException** – encapsulates one error condition
- **CarRentalService** – single responsibility: manage availability and bookings

This means if a requirement changes such as "add pricing", only one class needs to be modified, not five.

### Open/Closed Principle (OCP)

The system is **open for extension, closed for modification**:

- Adding a new car type? Add an enum value. Service logic doesn't change.
- Adding pricing? Extend `Reservation` with a pricing field without touching `CarRentalService`.
- Adding a persistence layer? Introduce a repository interface; the service logic remains unchanged.

The service can be tested with mock data and a database swapped later

### Liskov Substitution Principle (LSP)

Custom exception properly extends `RuntimeException`. Any code catching `RuntimeException` can handle `NoAvailableCarException` without knowing the subtype – and it behaves as expected (throws when inventory is exhausted).

### Interface Segregation Principle (ISP)

The public API exposes only the methods clients need:

```java
reserveCar(...)              // book a car
cancelReservation(...)       // cancel a booking
getAvailableCarsCount(...)   // check availability
getAllReservations(...)      // admin/reporting view
```

### Dependency Inversion Principle (DIP)

The service depends on **abstractions**, not concrete classes:

```java
// Constructor receives a Map (abstraction), not a HashMap (concrete)
public CarRentalService(Map<CarType, Integer> initialInventory) { ... }

// Internal state uses collection interfaces
private final List<Car> cars;
private final List<Reservation> reservations;
```

If persistence is added later, a repository abstraction can be injected:

```java
public CarRentalService(Map<CarType, Integer> inventory, ReservationRepository repo) { ... }
```

The core logic doesn't change; only the data source swaps out.

---

## Testing – Proving It Works

I followed a test-driven approach: write a failing test, implement the feature, refactor, repeat. The `CarRentalServiceTest` uses JUnit 5 and covers 32 test cases across these areas:

| Area                  | Count | What It Proves                                         |
| --------------------- | ----- | ------------------------------------------------------ |
| Core reservations     | 6     | Happy path: book a car, get back a valid Reservation   |
| Inventory limits      | 4     | Booking fails once all cars of a type are taken        |
| Overlap detection     | 4     | Partial, full, and adjacent overlaps handled correctly |
| Input validation      | 4     | Null/invalid inputs rejected with clear messages       |
| Cancellation          | 3     | Cancel works; cancelled cars become available again    |
| Availability queries  | 3     | `getAvailableCarsCount()` returns accurate numbers     |
| Reservation retrieval | 3     | Car-specific and system-wide views both work           |
| Edge cases            | 2     | 1-day and 365-day bookings both succeed                |

To run the full suite:

```bash
mvn clean test
```

## AI Tools & Prompt Engineering

I want to be transparent about how I'd approach this with AI assistance, because this reflects how professional development increasingly works and I've thought carefully about it.

### The Core Lesson: Treat AI Like a Contractor

The quality of AI output is almost entirely determined by the quality of your brief. A vague prompt produces vague code. A structured specification produces structured, reviewable code.

**Weak prompt:**

> "Write a car rental system in Java"

You would get one or two classes, no date handling, no fleet limits enforced, tests that are optional. The AI invents requirements and architecture as it goes confidently and often incorrectly.

**Strong prompt** (spec document style):

```markdown
## Project Overview

Car Rental System in Java 21 with Maven and JUnit 5.

## Domain Model (Required Classes)

| Class                   | Responsibility                           |
| ----------------------- | ---------------------------------------- |
| CarType                 | Enum: SEDAN, SUV, VAN                    |
| Car                     | Single vehicle with unique ID and type   |
| Reservation             | Immutable: car, start datetime, duration |
| NoAvailableCarException | Custom exception for failed bookings     |
| CarRentalService        | Entry point – fleet + reservations       |

## Functional Requirements

1. Reserve a car by type at a LocalDateTime for N days
2. Fleet limits per type (e.g. 3 Sedans, 2 SUVs, 2 Vans)
3. Reject overlapping reservations on the same car
4. Failed reservation throws NoAvailableCarException
5. Adjacent bookings (car returned Day 5, new reservation starts Day 5) succeed

## Test Requirements

- Successful reservation returns a Reservation object
- Fails when all cars of a type are booked for that period
- Boundary condition: Day 5 end + Day 5 start succeeds
- Different car types managed independently
- Back-to-back reservations on same car succeed

## Known Gaps (to list after implementation)

- No persistence layer
- No concurrency handling beyond basic thread safety
- No pricing logic
```

### Why Structured Prompts Win

| Technique               | Why It Matters                                                             |
| ----------------------- | -------------------------------------------------------------------------- |
| Tech stack table        | Locks out arbitrary choices (e.g. `java.util.Date` instead of `java.time`) |
| Named domain classes    | Architecture is fixed before code is written                               |
| Explicit boundary test  | AI won't think about Day 5 overlaps without being told                     |
| 'List gaps' instruction | Forces the model to reason critically instead of declaring completion      |
| Spec format (not prose) | AI treats it as truth throughout, not just the first message               |

### Validation Approach

Even with a perfect prompt, output needs review:

1. **Read the tests before the code.** If the "no availability" test doesn't fill the fleet first, it's testing nothing.
2. **Check overlap logic manually.** Draw a timeline. Verify the boundary condition matches the spec.
3. **Look for shared mutable state.** AI often puts fleet data in a static field, causing test bleed.
4. **Sabotage the tests.** Remove the availability check and confirm a test breaks. If nothing fails, coverage is fake.
5. **Compile immediately.** Run `mvn compile` after every step – catching errors early prevents compounding issues.

**The headline:** AI is only as good as the spec you give it. A conversational prompt produces conversational code. A structured spec produces structured, reviewable software.

## Known Limitations & Next Steps

Given the two-hour timebox, the solution is deliberately simple. I know exactly what's missing:

| Gap                    | What Production Would Do                             |
| ---------------------- | ---------------------------------------------------- |
| **In-memory only**     | Repository layer backed by PostgreSQL or Azure SQL   |
| **No modification**    | Update/extend endpoints alongside cancel             |
| **Basic concurrency**  | DB transactions with optimistic locking              |
| **Static inventory**   | Config-driven or runtime add/remove via admin API    |
| **No REST API**        | Spring MVC controllers with OpenAPI spec             |
| **No pricing**         | Rate tables per CarType, duration-based calculations |
| **No customer model**  | User entity, authentication, history tracking        |
| **Test coverage gaps** | Property-based tests for overlap logic, load tests   |

The architecture is ready for these extensions. The service layer has no framework dependencies, the domain model is clean, and the test suite provides a safety net for adding features.

## What This Maps To – CRD's Needs

| Criterion            | How Addressed                                                                      |
| -------------------- | ---------------------------------------------------------------------------------- |
| **OOP principles**   | Clean separation: enum → immutable value object → service layer → custom exception |
| **Requirements met** | All three core requirements satisfied and proven by tests                          |
| **Unit tests**       | 32 comprehensive tests covering happy path, edge cases, errors                     |
| **AI awareness**     | Structured prompt engineering + clear validation methodology                       |
| **Gap awareness**    | Known limitations documented and understood, not hidden                            |
| **Tech alignment**   | Java 21, Maven, Spring-ready architecture, event-driven design                     |

---

## How to Run

```bash
# Compile the project
mvn compile

# Run all unit tests
mvn clean test

# Run a specific test class
mvn test -Dtest=CarRentalServiceTest
```
