# Deepen Backend

Spring Boot backend service for the "Transparency for Patients at Home" application.

## Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot 3.2
- **Database:** PostgreSQL (H2 for development)
- **Authentication:** JWT
- **Containerization:** Docker

## Project Structure

```
server/
├── src/main/kotlin/com/deepen/
│   ├── config/          # Security & app configuration
│   ├── controller/      # REST API endpoints
│   ├── dto/             # Data Transfer Objects
│   ├── model/           # JPA entities
│   ├── repository/      # Data access layer
│   ├── security/        # JWT authentication
│   └── service/         # Business logic
├── src/main/resources/
│   ├── application.yml      # Dev configuration
│   └── application-prod.yml # Production configuration
├── Dockerfile
└── docker-compose.yml
```

## Prerequisites

- JDK 17+
- Gradle 8.5+ (or use the wrapper)
- Docker & Docker Compose (for containerized deployment)

## Running Locally (Development)

### Using Gradle

```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

The app will start on `http://localhost:8080` with an in-memory H2 database.

### H2 Console (Dev only)

Access at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:deependb`
- Username: `sa`
- Password: (empty)

## Running with Docker

```bash
# Build and start all services
docker-compose up --build

# Stop services
docker-compose down
```

## API Endpoints

### Health Check
- `GET /api/health` - Service health status

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Appointments
- `GET /api/appointments/{id}` - Get appointment by ID
- `GET /api/appointments/patient/{patientId}` - Get patient's appointments
- `GET /api/appointments/staff/{staffId}` - Get staff's appointments (staff only)
- `POST /api/appointments` - Create appointment (staff only)
- `PATCH /api/appointments/{id}` - Update appointment
- `DELETE /api/appointments/{id}` - Cancel appointment

### Care Tasks
- `GET /api/tasks/{id}` - Get task by ID
- `GET /api/tasks/patient/{patientId}` - Get patient's tasks
- `GET /api/tasks/patient/{patientId}/date/{date}` - Get tasks by date
- `POST /api/tasks` - Create task (staff only)
- `PATCH /api/tasks/{id}` - Update task
- `POST /api/tasks/{id}/complete` - Mark task complete

## User Roles

| Role | Description |
|------|-------------|
| `PATIENT` | Home care patient |
| `FAMILY_MEMBER` | Patient's family member |
| `DOCTOR` | Hospital doctor |
| `NURSE` | Hospital nurse |

## Environment Variables (Production)

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | deependb |
| `DB_USERNAME` | Database user | postgres |
| `DB_PASSWORD` | Database password | postgres |
| `JWT_SECRET` | JWT signing key (min 32 chars) | - |

## Next Steps

1. [ ] Add data seeding for development
2. [ ] Implement hospital staff management endpoints
3. [ ] Add scheduling logic for appointments
4. [ ] Connect to Firebase for additional features
5. [ ] Add unit and integration tests
