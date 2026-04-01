# Interview Preparation: Multiple Databases with Hibernate Caching

## Core Concepts To Explain Clearly

- Why split `users` and `orders` into different datasources
- Why each datasource needs its own `DataSource`, `EntityManagerFactory`, and `PlatformTransactionManager`
- Why one datasource is marked `@Primary`
- How `@EnableJpaRepositories` binds each repository package to the correct entity manager
- The difference between transaction boundaries and datasource selection

## Key Interview Questions

1. How do you configure multiple databases in Spring Boot with JPA?
   Mention separate config classes, separate repository packages, and separate transaction managers.

2. Why can't one `EntityManagerFactory` manage entities from different databases cleanly?
   Because each persistence unit is tied to a specific datasource and dialect behavior.

3. What does `entityManagerFactoryRef` do?
   It tells Spring Data which entity manager factory should back a given repository package.

4. What does `transactionManagerRef` do?
   It binds repository operations to the correct transaction manager for that datasource.

5. Why is `@Primary` important in multi-datasource applications?
   It helps Spring resolve shared infrastructure beans when more than one candidate exists.

6. What happens if a repository package points to the wrong datasource config?
   Repositories may fail to initialize, use the wrong database, or produce missing-table errors.

7. How would you handle a use case that spans MySQL and PostgreSQL in one business operation?
   Explain local transactions first, then mention XA or distributed transactions only when truly required.

8. What is Hibernate second-level cache?
   It is a SessionFactory-level cache shared across sessions for entity and state reuse.

9. What is the query cache?
   It caches query result identifiers, not the full entity state by itself.

10. Why do you usually combine query cache with second-level cache?
    Because cached query ids are most useful when entity state is also cached.

11. What is the role of Ehcache here?
    It is the JCache provider used by Hibernate for cache region storage.

12. What are common risks of Hibernate caching?
    Stale data, invalidation complexity, memory pressure, and poor cache-region sizing.

## Project-Specific Talking Points

- `User` is stored in MySQL and `Order` is stored in PostgreSQL.
- `OrderServiceImpl` validates the user through the user repository, then persists the order through the order repository.
- `User` and `Order` are both annotated with `@Cacheable` and Hibernate `READ_WRITE` cache strategy.
- Query caching is enabled globally in `application.yml`.
- `findByEmail` is a good example to discuss query caching.

## Troubleshooting Checklist

- Check repository package names in `@EnableJpaRepositories`
- Check the bean names used in `entityManagerFactoryRef` and `transactionManagerRef`
- Verify JDBC URLs, dialect resolution, and driver dependencies
- Confirm only the intended datasource is `@Primary`
- Confirm cache provider and `hibernate-jcache` are on the classpath
- Check whether stale data is caused by cache settings or missing invalidation
- Verify timezone and connection properties when databases behave differently across environments

## Practical Scenarios To Practice

- Add a third datasource for reporting
- Move `User` reads to read-only transactions
- Add dedicated cache regions with tuned TTL and heap sizes
- Disable query cache for frequently changing queries
- Document grouped APIs per module in Swagger UI
- Explain why a cross-database write flow might need compensating logic instead of XA

## Quick Revision Answers

- Multiple databases: separate datasource beans and separate persistence units
- Correct repository routing: `@EnableJpaRepositories`
- Correct transaction routing: `@Transactional(transactionManager = "...")`
- Second-level cache: shared entity cache
- Query cache: cached ids or result metadata
- Best cache candidates: read-heavy, low-churn data
- Poor cache candidates: highly volatile transactional data
