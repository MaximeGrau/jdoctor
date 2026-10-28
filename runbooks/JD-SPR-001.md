# JD-SPR-001: HikariCP pool saturated

## What it means

Request threads are waiting in `HikariPool.getConnection` because every connection is in use. When
the wait exceeds `connection-timeout` (30 s by default), requests fail with `SQLTransientConnectionException`.

## How to confirm

1. The evidence shows how many threads wait in each dump and which application code asks for connections.
2. Enable leak detection to log the code that holds connections too long:
   `spring.datasource.hikari.leak-detection-threshold=2000` (milliseconds).
3. Expose pool metrics with Actuator and Micrometer: `hikaricp.connections.active`, `hikaricp.connections.pending`.

## Common causes in Spring applications

- Remote HTTP calls, message sends or `Thread.sleep` inside a `@Transactional` method: the connection is held the whole time.
- Slow queries (missing index, N+1 selects).
- `spring.jpa.open-in-view=true` (the default): the connection stays open while the view renders.
- More request threads (`server.tomcat.threads.max`, 200 by default) than connections (`maximum-pool-size`, 10 by default).

## How to fix

Shorten transactions first: move remote calls out of `@Transactional`, fix slow queries, set
`spring.jpa.open-in-view=false`. Then size the pool with the database's capacity in mind; raising
`maximum-pool-size` alone often moves the bottleneck to the database.
