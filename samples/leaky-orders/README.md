# leaky-orders

A Spring Boot app with bugs on purpose, to try jdoctor on something realistic:

| Bug | Where | jdoctor finding |
| --- | --- | --- |
| A cache that never evicts | `OrderCache` | `JD-MEM-001` heap grows after GC |
| A slow call inside `@Transactional` with a pool of 2 | `ReportService` | `JD-SPR-001` HikariCP pool saturated |
| `GET /orders/{id}/size` dereferences null for an unknown id | `OrderController.size` | `JD-EXC-001` NullPointerException |
| `GET /orders/{id}` throws an exception that nothing maps to 404 | `OrderController.get` | `JD-EXC-002` unhandled application exception |

Startup failures, one Spring profile each (classes in `failures/`):

| Profile | Bug | jdoctor finding |
| --- | --- | --- |
| `init-crash` | NullPointerException in a `@PostConstruct` method | `JD-START-001` |
| `duplicate-bean` | Two `@Bean` methods named `priceRounding` | `JD-START-002` |
| `two-candidates` | Two `TaxPolicy` beans, injected without a qualifier | `JD-START-003` |
| `missing-bean` | `PaymentGateway` has no implementation | `JD-START-004` |
| `cycle` | Two services that need each other | `JD-START-005` |

## Run the demo

```bash
# 1. Start the app with a small heap
cd samples/leaky-orders
gradle bootJar
java -Xmx256m -jar build/libs/leaky-orders-0.0.1.jar

# 2. In a second terminal, send traffic
./load.sh

# 3. In a third terminal, diagnose (from the repository root)
./gradlew :jdoctor-cli:installDist
jdoctor-cli/build/install/jdoctor/bin/jdoctor ps
jdoctor-cli/build/install/jdoctor/bin/jdoctor analyze pid:<pid> --duration 40s
```

## Try the log rules

```bash
# A startup failure: the app stops, jdoctor reads the log
java -jar build/libs/leaky-orders-0.0.1.jar --spring.profiles.active=two-candidates > startup.log 2>&1
jdoctor analyze startup.log

# Runtime exceptions: call the buggy endpoints, then analyze the log with the sources
java -jar build/libs/leaky-orders-0.0.1.jar > app.log 2>&1 &
curl localhost:8080/orders/$(uuidgen)/size; curl localhost:8080/orders/$(uuidgen)
jdoctor analyze app.log --explain --source .
```

The logs in `jdoctor-analyzers/src/test/resources/logs/` were captured this way.

## Expected results

After 40 seconds of load: `JD-MEM-001` (critical, the heap floor grows by about
170 MiB/min) and `JD-SPR-001` (warning, 6 threads waiting for a connection, from
`ReportService.monthlyReport`).
