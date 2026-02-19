# Deepen Backend

Spring Boot backend service for the "Transparency for Patients at Home" application.

## Tech Stack

- **Language:** Kotlin
- **Framework:** Spring Boot 3.2
- **Database:** PostgreSQL (H2 for development)
- **Migrations:** Flyway (V1-V3)
- **Authentication:** JWT
- **Containerization:** Docker

## Key Features

- **Role-based access** (PATIENT, FAMILY_MEMBER, DOCTOR, NURSE)
- **Appointments** CRUD
- **Weekly auto-scheduling** (Schedule Plans)
  - Generates a weekly draft plan and supports confirmation/locking
  - Enforces workload policies (daily/weekly limits)
  - Ensures **minimum office coverage** during business hours

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
│   ├── application.yml          # Dev configuration (H2)
│   ├── application-prod.yml     # Production configuration (PostgreSQL)
│   └── db/migration/            # Flyway migrations (V1, V2, V3)
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

The Docker setup runs the backend with the `prod` profile against PostgreSQL and applies Flyway migrations automatically:

- **V1**: schema
- **V2**: initial staff/patient/family demo data
- **V3**: scheduling tables + additional demo data used by the auto-scheduling engine

If you want a fresh database state:

```bash
docker-compose down -v
docker-compose up --build
```

## Test Accounts (Docker/Flyway)

All accounts created by migrations use the password:

- **Password:** `password`

Examples:

- **Doctor**: `dr.sarah.johnson@hospital.com`
- **Doctor**: `dr.michael.chen@hospital.com`
- **Nurse**: `nurse.emily.davis@hospital.com`
- **Nurse**: `nurse.james.wilson@hospital.com`
- **Patient**: `patient.john.smith@email.com`
- **Family**: `family.jane.smith@email.com`

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

### Schedule Plans (Weekly Auto-Scheduling)

- `POST /api/schedule-plans/generate` - Generate a weekly plan (DRAFT)
- `POST /api/schedule-plans/{planId}/confirm` - Confirm and lock the plan (CONFIRMED)
- `GET /api/schedule-plans/{planId}` - Get plan details + appointments
- `GET /api/schedule-plans/week/{weekStartDate}` - Get plan by week start date
- `GET /api/schedule-plans/{planId}/summary` - Staff breakdown summary
- `GET /api/schedule-plans` - List all plans

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

## Notes

- Local development uses H2 by default; Docker uses PostgreSQL + Flyway.
- The auto-scheduling engine requires the Flyway scheduling tables/data (V3) when running against PostgreSQL.
