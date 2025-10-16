# Simple Contacts Manager

Modern Spring Boot address book with authentication, MySQL persistence, Kafka integration, Liquibase migrations, AJAX/Bootstrap UI, and a companion HTTP weather microservice. Everything targets Java 21 and is container-ready for local development.

## Features
- User registration & login (Spring Security, BCrypt). Sign-up emits Kafka `user-signups` events consumed in-app.
- Contact CRUD with ownership rules: listing is public, but create/update/delete/export require login.
- MySQL persistence backed by Liquibase change sets for deterministic schema management.
- AJAX-first Bootstrap UI: search, modals for editing, CSV export, and inline weather badges.
- Weather microservice provides location-based mock weather data over HTTP; contacts service calls it on demand.
- Dockerfiles for each service plus a `docker-compose.yml` orchestrating MySQL, Kafka, Zookeeper, weather, and contacts services.

## Project Structure
```
.
├── pom.xml                        # Parent Maven POM (Java 21, Spring Boot 3.3)
├── contacts-service/              # Main web app (REST API + UI)
│   ├── src/main/java/com/example/contacts/
│   ├── src/main/resources/
│   └── Dockerfile
├── weather-service/               # Weather microservice (HTTP JSON)
│   ├── src/main/java/com/example/weather/
│   ├── src/main/resources/
│   └── Dockerfile
├── docker-compose.yml             # Local dev stack (MySQL + Kafka + services)
└── docs/ARCHITECTURE.md           # High-level overview
```

## Prerequisites
- Java 21 (e.g., Temurin/OpenJDK).
- Maven 3.9+ (or use the Docker build steps).
- Docker Desktop 4.x (for container workflow).

Configure environment variables as needed (see below) before launching `spring-boot:run`. Without overrides, the app expects local MySQL/Kafka instances on default ports.

### Environment Variables
| Variable | Default                 | Description |
|----------|-------------------------|-------------|
| `MYSQL_HOST` | `localhost`             | MySQL hostname |
| `MYSQL_PORT` | `3307`                  | MySQL port |
| `MYSQL_DATABASE` | `contacts_db`           | Database name (created automatically) |
| `MYSQL_USER` | `contacts_user`         | DB user |
| `MYSQL_PASSWORD` | `contacts_pass`         | DB password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`        | Kafka bootstrap servers |
| `KAFKA_SIGNUP_TOPIC` | `user-signups`          | Kafka topic for sign-up events |
| `WEATHER_SERVICE_URL` | `http://localhost:9000` | Weather microservice base URL |

Liquibase runs automatically on startup and creates required tables.

## Build & Run (Docker Compose)
```bash
# Build and start the entire stack
docker compose up --build

# Tear down
docker compose down
```
Services:
- Weather microservice: http://localhost:9000
- Contacts UI/API: http://localhost:9001
- Kafka broker: localhost:9092
- MySQL: localhost:3307 (credentials from table above)

The compose file auto-creates Kafka topics and waits for MySQL health before starting the app. MySQL data persists in the `mysql_data` named volume.

### Useful Kafka Commands
```bash
# Exec into Kafka container and list topics
docker compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Tail sign-up events
docker compose exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic user-signups \
  --from-beginning
```

## Application Walkthrough
1. Visit `http://localhost:9001` to browse public contacts (empty on first run).
2. Create an account via `/signup` (AJAX form). This triggers a Kafka `user-signups` message.
3. Sign in at `/login`, then add/edit/delete contacts directly from the AJAX UI.
4. Use search to filter by name and export your contacts as CSV.
5. Weather badges next to each contact are fetched from the weather microservice.

## Testing
- Unit tests are currently minimal; extend using Spring Boot’s test harness (`spring-boot-starter-test`, `spring-security-test` already included).
- Run `mvn test` in either module to execute new tests.

## Linting & Formatting
- Java code follows standard Spring conventions; leverage your IDE’s formatter or `spotless` if desired.
- Static assets are simple vanilla JS and Bootstrap; tweak under `contacts-service/src/main/resources/static`.

## Troubleshooting
- **MySQL connection errors**: confirm credentials and that the database is reachable. Liquibase logs schema changes on startup; review application logs for hints.
- **Kafka timeouts**: verify broker availability and topic name (`user-signups`). Default settings auto-create the topic in Docker Compose.
- **Weather service offline**: the contacts UI gracefully degrades, but ensure `WEATHER_SERVICE_URL` points to a reachable endpoint for full experience.

---

