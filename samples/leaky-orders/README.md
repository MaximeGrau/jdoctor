# leaky-orders

A Spring Boot app with two bugs on purpose, to try jdoctor on something realistic:

| Bug | Where | jdoctor finding |
| --- | --- | --- |
| A cache that never evicts | `OrderCache` | `JD-MEM-001` heap grows after GC |
| A slow call inside `@Transactional` with a pool of 2 | `ReportService` | `JD-SPR-001` HikariCP pool saturated |

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

Expected result after 40 seconds of load: `JD-MEM-001` (critical, the heap floor grows by about
170 MiB/min) and `JD-SPR-001` (warning, 6 threads waiting for a connection, from
`ReportService.monthlyReport`).
