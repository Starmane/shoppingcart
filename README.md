# Shopping Cart Microservice System

A small retail Shopping Cart system built as a set of Spring Boot microservices, demonstrating cart management, asynchronous event-driven statistics, and a reverse-proxy entry point.

## Requirements

To build and run this project you need:

- **Docker** and **Docker Compose** (the entire system is run via a single `docker compose up --build` command)
- **Java 21** (only required if building/running modules outside Docker, e.g. for local development in an IDE)
- **Maven 3.9+** (only required for local builds outside Docker)
- **IntelliJ IDEA** (or any Java IDE) - recommended for browsing/editing the source, with the **Lombok plugin** enabled (Settings → Plugins → Lombok, and enable annotation processing under Settings → Build → Compiler → Annotation Processors)
- **Postman** (optional) - a collection of example requests is provided in the project folder for easier manual testing

No local installation of MongoDB or Kafka is required - both run as Docker containers as part of the compose setup.

## Tools Used and Their Purpose

| Tool | Purpose |
|---|---|
| **Java 21** | Application language. Used with virtual threads enabled (see notes below). |
| **Spring Boot** | Application framework for both microservices - REST APIs, dependency injection, configuration. |
| **Spring Data MongoDB** | Persistence layer for both cart data and cart event/stats data. |
| **Spring Kafka** | Asynchronous messaging between cart-service (producer) and stats-service (consumer). |
| **MongoDB** | Database for cart documents and cart event records. |
| **Apache Kafka (KRaft mode)** | Message broker for propagating cart actions (ADD/MODIFY/DELETE) from cart-service to stats-service, without requiring Zookeeper. |
| **Lombok** | Reduces boilerplate (getters/setters/constructors/equals/hashCode) on model and DTO classes. |
| **springdoc-openapi / Swagger UI** | Auto-generated, interactive API documentation for both services. |
| **Spring Boot Actuator** | Health check and basic application insight endpoints, used by Docker health checks. |
| **Nginx** | Reverse proxy acting as a single entry point ("API gateway" stand-in) routing requests to the appropriate microservice. |
| **Docker / Docker Compose** | Containerization and orchestration of all services and infrastructure (Mongo, Kafka, cart-service, stats-service, Nginx). |

## Architecture Overview

```
                     ┌─────────────────────┐
   Client  ────────▶ │  Nginx (port 8000)   │
                     │  reverse proxy        │
                     └──────────┬───────────┘
                  ┌──────────────┴──────────────┐
                  ▼                              ▼
        ┌───────────────────┐         ┌───────────────────┐
        │  cart-service       │         │  stats-service      │
        │  port 9090          │  Kafka  │  port 9091          │
        │                      │ ──────▶ │                      │
        │  /carts/**           │ cart-   │  /stats              │
        └─────────┬────────────┘ events  └─────────┬────────────┘
                  │                                  │
                  └──────────────┬───────────────────┘
                                  ▼
                          MongoDB (single instance)
                       carts / cart_events collections
```

## Modules

The project consists of **three Maven modules**:

- **`cart-service`** - owns cart state. Provides endpoints to view a cart, add/modify/remove items, and evict an entire cart. Publishes an event to Kafka for every add/modify/remove action.
- **`stats-service`** - consumes cart events from Kafka and stores them as `cart-events` in MongoDB. Provides an endpoint to query statistics - how many offers of a given id and action occurred within a given time period.
- **`common`** - a shared module containing classes/enums used by both services (e.g. the Kafka message contract, `ActionType`, price-related enums).

## How Cart Actions Flow to Statistics

When a user adds, modifies, or removes an item from their cart via cart-service:

1. The cart document in MongoDB is updated immediately (the user gets an immediate response).
2. Cart-service **asynchronously** publishes an event describing the action (offer id, action type, prices, timestamp) to a Kafka topic.
3. Stats-service consumes this event and persists it as a `cart-event` document in MongoDB.
4. The `/stats` endpoint on stats-service queries these stored cart-events to answer "how many offers of a particular id and action occurred in a particular period".

This means cart operations are never blocked or slowed down by the statistics pipeline - if Kafka or stats-service is temporarily unavailable, cart operations still succeed, and statistics simply catch up once the dependency recovers.

## Data Storage

- **Cart data** (the `carts` collection) and **cart-event data** (the `cart_events` collection) are both stored in **MongoDB**, currently in the **same database**, under different collections.
- Kafka itself does not act as long-term storage - it is purely the transport mechanism between cart-service and stats-service. The durable record of statistics lives in MongoDB.

## Ports

| Service | Port | Notes |
|---|---|---|
| **Nginx (common entry point)** | `8000` | Single entry point for all API traffic; routes to cart-service or stats-service based on path. |
| **cart-service** | `9090` | Cart CRUD API + Swagger UI. |
| **stats-service** | `9091` | Stats query API + Swagger UI. |
| **MongoDB** | `27017` | Exposed for local inspection (e.g. via `mongosh` or MongoDB Compass). |
| **Kafka** | `9092` | Exposed for local inspection (e.g. via `kafka-console-consumer`). |

> **Note:** if any of these ports are already in use on your host machine, you'll need to adjust the port mappings in `docker-compose.yml` before starting the system.

## Running the Project

From the project root:

```bash
docker compose up
```

This single command builds and starts:
- MongoDB
- Kafka (KRaft mode, single broker)
- cart-service
- stats-service
- Nginx (reverse proxy / entry point)

Once everything is up and healthy, the API is reachable via Nginx at `http://localhost:8000`, or directly against each service at `http://localhost:9090` and `http://localhost:9091`.

## API Documentation (Swagger)

Interactive API docs are available once the services are running:

- cart-service: [http://localhost:9090/swagger-ui/index.html](http://localhost:9090/swagger-ui/index.html)
- stats-service: [http://localhost:9091/swagger-ui/index.html](http://localhost:9091/swagger-ui/index.html)

## Postman Collection

A Postman collection with example requests for all endpoints (add/modify/remove cart items, evict cart, query stats) is included in the project folder for convenient manual testing.

## Health Checks

Both services expose **Spring Boot Actuator** endpoints (`/actuator/health`, `/actuator/info`), used by Docker Compose health checks to determine readiness and liveness. These can also be queried manually to check the status of MongoDB and Kafka connectivity from each service's perspective.

## Design Decisions and Simplifications

Most design choices in this project were made **for the sake of simplicity**, given the scope of the task. The following are known simplifications and possible future improvements:

### Infrastructure / Deployment
- **Single instance per service.** In a real-world scenario, cart-service and stats-service would be horizontally scaled across multiple containers/instances to distribute traffic and provide redundancy. A managed container orchestration platform (e.g. **AWS ECS** or **AWS EKS**) would be a good fit for this.
- **Nginx as a reverse proxy / API gateway stand-in.** Nginx is used here to provide a single entry point (port 8000) that routes to the appropriate service. In a real-world deployment this would more likely be implemented using a dedicated API gateway (e.g. Spring Cloud Gateway, AWS API Gateway, Kong) offering richer routing, auth integration, and rate limiting.
- **Kafka replication factor of 1.** For this single-broker example, the `cart-events` topic has a replication factor of 1. For production scalability and durability, this would be increased (typically 3) alongside running a multi-broker Kafka cluster.

### Security
- **No authentication/authorization layer.** This was outside the scope of the assignment. In a real-world scenario, an authentication mechanism (e.g. JWT-based) would be required, along with rate limiting and an identity/access management solution.
- **Actuator endpoints should have reduced visibility.** Currently, `/actuator/health` (and related endpoints) are exposed with detailed information for ease of debugging. In a production-facing configuration, the set of exposed actuator endpoints and the level of detail shown should be restricted (e.g. via `management.endpoint.health.show-details` and `management.endpoints.web.exposure.include`).

### Data & Modeling
- **Shared database, separate collections.** Cart data and cart-event data currently live in the same MongoDB database under different collections (`carts` and `cart_events`). In a larger application, these would likely be split into separate databases, following a one-database-per-microservice principle for stronger ownership boundaries.
- **`common` module introduces minor coupling.** Sharing enums and the Kafka message contract via a `common` module means both services depend on the same shared artifact - a deployment of one technically requires awareness of the other's contract version. The shared types are intentionally simple (enums and a flat message DTO) to keep this coupling minimal, but it's worth noting as a tradeoff versus fully independent per-service contracts (e.g. via a schema registry).

### Observability
- **Logging is largely missing.** Beyond default Spring Boot startup logging and a small amount of debug logging used during development, the application does not implement structured application-level logging. For a real system, meaningful logging (request tracing, error context, correlation IDs across the Kafka boundary) would be added.
- **No monitoring/observability stack.** Tools such as **Prometheus + Grafana**, the **ELK stack**, or **Splunk** would be added for metrics collection, log aggregation, dashboards, and alerting.

### Code Quality & CI/CD
- **No CI/CD pipeline.** A real project would include automated build/test/deploy pipelines (e.g. GitHub Actions, GitLab CI, Jenkins).
- **No static analysis/linting.** Tools such as **SonarQube** (code quality, code smells, coverage) and **Snyk** (dependency vulnerability scanning) would be integrated into the build pipeline.
- **No `.gitignore`.** This project is not currently version-controlled as a shared repository, so a `.gitignore` was not added. One would be required (excluding `target/`, IDE files, etc.) before pushing to source control.

### API Design
- **No API versioning.** Endpoints are currently unversioned (e.g. `/carts`, `/stats`). A future improvement would introduce versioning, e.g. `/v1/carts`, `/v1/stats`, to allow non-breaking evolution of the API over time.

### Performance
- **Virtual threads are enabled** (Java 21 `spring.threads.virtual.enabled: true`) in both services. At the scale of this project (local testing, low traffic), this provides no measurable benefit. However, in a larger-scale application handling many concurrent blocking I/O operations (database calls, Kafka publishes), virtual threads can meaningfully improve throughput and resource efficiency without significant code changes.

### Language Choice
- **Kotlin could reduce boilerplate further.** While Lombok significantly reduces Java boilerplate in this project, Kotlin would remove the need for Lombok entirely (data classes, built-in null-safety) and offers additional benefits such as compile-time null-checking, which would help prevent a class of bugs around the "all attributes mandatory" validation rules in this domain.

## Summary of Possible Future Improvements

- Horizontal scaling of cart-service and stats-service (multiple instances/containers)
- Replace Nginx with a proper API gateway
- Increase Kafka replication factor for durability
- Add authentication (JWT), rate limiting, and identity management
- Reduce Actuator endpoint exposure for production
- Separate databases per microservice (as of now they are in the same database under different collections)
- Reduce/remove `common` module coupling, or formalize the shared contract via a schema registry
- Add structured logging and correlation IDs
- Add monitoring/observability (Prometheus, Grafana, ELK, or Splunk)
- Add CI/CD pipelines
- Add static analysis and dependency scanning (SonarQube, Snyk)
- Add a `.gitignore` before version-controlling the project
- Introduce API versioning (`/v1/carts`, `/v1/stats`)
- Consider Kotlin for reduced boilerplate and improved null-safety
- Organize test files by folders when new test requirements are introduced