# Architecture Overview

## Modules
- **contacts-service** – Spring Boot 3.3 (Java 21) web tier. Serves pages, exposes REST APIs, persists data in MySQL via Spring Data JPA, manages Liquibase migrations, integrates with Kafka, and orchestrates weather lookups via HTTP.
- **weather-service** – Lightweight Spring Boot microservice exposing `/api/weather` to provide mock weather data per address. Contacts service performs outbound HTTP calls to this service for each rendered contact.

Both modules share the parent POM at the repository root.

## Data Flow
1. **Authentication**
   - Anonymous users can browse `/` and the contact listing via `GET /api/contacts`.
   - Registration (`POST /api/auth/signup`) persists a new user, hashes their password, fires a `user-signups` Kafka event, and authenticates them.
   - Login uses the custom Bootstrap form at `/login`, handled by Spring Security form login with session cookies.
2. **Contact Management**
   - Contact CRUD endpoints live under `/api/contacts`.
   - Create, update, and delete routes require an authenticated session and restrict access to the contact owner.
   - CSV export (`GET /api/contacts/export`) is protected by authentication.
3. **Weather Lookup**
   - For every contact returned to the UI, the contacts service calls `weather-service` over HTTP using Spring's `RestClient`.
   - Weather responses are cached briefly in-memory to keep the UI responsive.
4. **Kafka Integration**
   - `KafkaTemplate` publishes `SignupEvent` messages to the `user-signups` topic after a successful registration.
   - `SignupEventListener` consumes messages and currently logs them, acting as a placeholder for downstream processing.

## Persistence
- MySQL schema managed with Liquibase change sets (`db/changelog/db.changelog-master.yaml`).
- Tables:
  - `users` – stores credentials and role.
  - `contacts` – stores contact data and foreign key to `users`.

## Front-end
- Bootstrap-driven single page under `/index.html`.
- JavaScript (vanilla + Fetch API) delivers an AJAX UX for listing, searching, CRUD, and CSV export without full page reloads.
- Login/signup views live under `/templates` and reuse Bootstrap styling.

## Containerization & Deployment
- Each module ships with its own Dockerfile using Eclipse Temurin JDK 21 base images.
- Repository-level `docker-compose.yaml` orchestrates MySQL, Zookeeper, Kafka, contacts-service, and weather-service for local/dev usage.
