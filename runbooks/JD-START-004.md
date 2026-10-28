# JD-START-004: required bean not found

## What it means

A constructor or field needs a bean that no configuration creates in this context.

## How to confirm

1. The evidence names the injection point and the missing type.
2. Run with `--debug` to see which auto-configurations were skipped and why.

## Common causes in Spring applications

- An interface with no implementation, or an implementation without `@Component` / `@Service`.
- An implementation outside the packages scanned from the `@SpringBootApplication` class.
- A bean behind a `@Profile` or `@ConditionalOnProperty` that is not active.
- A missing starter dependency, so the auto-configuration that would create the bean is not there.

## How to fix

Provide the bean: annotate the implementation, move it under the application package, activate the
profile or property, or add the starter. If the dependency is optional, inject `Optional<Type>` or
`ObjectProvider<Type>`.
