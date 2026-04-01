# Order Management System

Spring Boot 4 sample application that separates `users` and `orders` across two relational databases while enabling Hibernate second-level and query caching with Ehcache.

## What This Project Shows

- Multiple datasources with separate JPA repositories, entity managers, and transaction managers
- MySQL-backed `users` module
- PostgreSQL-backed `orders` module
- Hibernate second-level cache and query cache with Ehcache
- OpenAPI and Swagger UI documentation with grouped docs for each module

## Tech Stack

- Java 21
- Spring Boot 4.0.5
- Spring Data JPA
- MySQL
- PostgreSQL
- Hibernate JCache + Ehcache
- springdoc-openapi

## Datasource Layout

- `spring.datasource.user` points to MySQL database `user_db`
- `spring.datasource.order` points to PostgreSQL database `order_db`
- [UserDBConfig.java](/v:/learning/2026/projects/springboot/order-management-system/src/main/java/com/viv/user/config/UserDBConfig.java) owns the user datasource, entity manager factory, and transaction manager
- [OrderDBConfig.java](/v:/learning/2026/projects/springboot/order-management-system/src/main/java/com/viv/order/config/OrderDBConfig.java) owns the order datasource, entity manager factory, and transaction manager

## API Documentation

After starting the application:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Users group: `http://localhost:8080/v3/api-docs/users`
- Orders group: `http://localhost:8080/v3/api-docs/orders`

## Available Endpoints

- `POST /users`
- `GET /users/{id}`
- `POST /orders?userId=1&amount=249.99`

## Local Setup

1. Start MySQL and PostgreSQL instances that match [application.yml](/v:/learning/2026/projects/springboot/order-management-system/src/main/resources/application.yml).
2. Ensure these databases exist:
   `user_db` in MySQL and `order_db` in PostgreSQL.
3. Run the application:

```bash
./mvnw spring-boot:run
```

4. Open Swagger UI to test the API interactively.

## Caching Notes

- Both `User` and `Order` entities are marked as cacheable.
- Hibernate query cache is enabled in [application.yml](/v:/learning/2026/projects/springboot/order-management-system/src/main/resources/application.yml).
- Ehcache configuration lives in [ehcache.xml](/v:/learning/2026/projects/springboot/order-management-system/src/main/resources/ehcache.xml).
- Repeated reads like `findByEmail` can benefit from query caching when the cache region is warm.

## Interview Preparation

See [INTERVIEW_PREPARATION_LIST.md](/v:/learning/2026/projects/springboot/order-management-system/INTERVIEW_PREPARATION_LIST.md) for project-specific interview questions and talking points on:

- multiple datasource setup
- entity manager and transaction manager separation
- Hibernate second-level cache
- query cache behavior
- common debugging and production tradeoffs
