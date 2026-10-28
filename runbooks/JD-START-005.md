# JD-START-005: circular dependency between beans

## What it means

Beans depend on each other in a loop, so none of them can be created first. Circular references
are refused by default since Spring Boot 2.6. The evidence shows the loop as Spring printed it.

## How to confirm

1. The evidence draws the cycle: each bean and the one it needs.
2. Look at the constructors of the beans in the loop.

## Common causes in Spring applications

- Two services that call each other, usually after a feature was split across them.
- A configuration class that injects a bean it also defines.

## How to fix

Break the loop in the design: move the shared logic into a third bean that both use, or replace a
direct call with an application event. `@Lazy` on one injection point works as a stopgap.
Avoid `spring.main.allow-circular-references=true`, which hides the problem.
