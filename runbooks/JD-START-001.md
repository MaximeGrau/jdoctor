# JD-START-001: application failed to start

## What it means

Spring Boot logged "Application run failed" and stopped, without a failure analysis of its own.
The root cause of the stack trace is what went wrong; the outer exceptions only say where Spring
was when it happened (creating a bean, running an init method, refreshing the context).

## How to confirm

1. The evidence lists the cause chain, outermost first, and the first application frame of the root cause.
2. Start the application locally with the same profiles and configuration: a startup failure reproduces every time.
3. Add `--debug` (or `debug=true`) to print the condition evaluation report when the failure depends on auto-configuration.

## Common causes in Spring applications

- A `NullPointerException` in a constructor or a `@PostConstruct` method: a setting or a dependency that is not there yet.
- A missing or malformed property bound to a `@ConfigurationProperties` class.
- A database, broker or remote service that is unreachable when a bean connects to it at startup.
- A Flyway or Liquibase migration that fails.

## How to fix

Fix the code at the frame shown first. If the value can legitimately be missing, give it a default
or validate it with `@Validated` on the `@ConfigurationProperties` class, so the next failure names
the property instead of throwing a `NullPointerException`. Avoid remote calls during bean creation;
do them lazily or after `ApplicationReadyEvent`.
