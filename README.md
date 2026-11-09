# CQRS and Event Sourcing with Axon

A Java 21 / Spring Boot example of a bank account service. Commands are handled by an event sourced Axon aggregate. Axon events update a separate JPA read model for account balances and operation history. Reads and writes have separate controllers and query/command messages.

## Requirements

- Java 21
- Maven (or the included Maven wrapper)
- Docker Compose only when using PostgreSQL or Axon Server

By default the app uses an in-memory Axon event store and H2 for the read model, so it starts without external services:

```shell
./mvnw spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

H2 console: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:accounts`, user `sa`, no password)

## Angular dashboard

The standalone Angular client lives in `frontend/`. It provides account creation and activation, credit/debit actions, a live operation feed, and account balance summaries. It proxies `/commands` and `/queries` to the backend on port `8080` during development.

Use Node.js `22.22.3` or newer, then start the UI in a second terminal:

```shell
cd frontend
npm install
npm start
```

Open <http://localhost:4200>. Angular's version compatibility and setup requirements are documented in the [official Angular docs](https://angular.dev/reference/versions).

The Axon in-memory event store is intended for a single process and loses events on restart. For a durable, distributed event store, run Axon Server and activate the Maven profile:

```shell
docker compose up -d axon-server postgres
./mvnw spring-boot:run -Paxon-server \
  -Dspring-boot.run.arguments="--AXON_SERVER_ENABLED=true --DB_URL=jdbc:postgresql://localhost:5432/accounts --DB_USERNAME=accounts --DB_PASSWORD=accounts --DB_DRIVER=org.postgresql.Driver"
```

Axon Server console: <http://localhost:8024>. The connector uses gRPC port `8124`.

## API

All write operations are commands. A create call returns the generated account ID; newly created accounts must be activated before money operations are accepted.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/commands/account/create` | Create an account with `initialBalance` and `currency` |
| `POST` | `/commands/account/{id}/activate` | Activate a created account |
| `POST` | `/commands/account/{id}/credit` | Credit `{ "amount": 100, "currency": "MAD" }` |
| `POST` | `/commands/account/{id}/debit` | Debit `{ "amount": 25, "currency": "MAD" }` |
| `GET` | `/queries/accounts` | List account projections |
| `GET` | `/queries/accounts/{id}` | Read an account projection |
| `GET` | `/queries/accounts/{id}/operations` | List credit and debit operations |
| `GET` | `/queries/accounts/{id}/watch` | Receive the initial account and live operation updates as server-sent events |

Example create body:

```json
{ "initialBalance": 500, "currency": "MAD" }
```

The read model is eventually consistent: a command is accepted by the event-sourced aggregate, then the projection catches up asynchronously.

## Design

- `commands/aggregates`: account business rules and event-sourced state.
- `commands/controllers`: write HTTP API and command dispatch.
- `common/commands`, `common/events`, `common/queries`: messages shared between application sides.
- `query/services`: event handlers maintain the read projection and query handlers serve reads.
- `query/entities` and `query/repositories`: relational read model and operation log.

The reference implementation and lesson flow are based on [mohamedYoussfi/cqrs-eventsourcing-axon](https://github.com/mohamedYoussfi/cqrs-eventsourcing-axon) and the linked Axon CQRS/event sourcing lesson.
