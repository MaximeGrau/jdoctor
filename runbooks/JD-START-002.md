# JD-START-002: two beans with the same name

## What it means

Two bean definitions use the same name, and bean overriding is disabled (the default since
Spring Boot 2.1). Spring Boot names both definitions in the evidence.

## How to confirm

1. The evidence names the bean and the two classes or configurations that define it.
2. Search the code for `@Bean` methods with that name, and for `@Component` classes whose
   default name (the class name with a lowercase first letter) collides.

## Common causes in Spring applications

- The same `@Bean` method copied into two `@Configuration` classes, often by generated code.
- Two classes with the same simple name in different packages, both annotated `@Component` or `@Service`.
- A library auto-configuration and an application configuration that both declare a bean with the same name.

## How to fix

Keep one definition. If both are needed, give them distinct names (`@Bean("checkoutRounding")`)
and inject them with `@Qualifier`. Do not set `spring.main.allow-bean-definition-overriding=true`
to make the error go away: which definition wins then depends on the loading order.
