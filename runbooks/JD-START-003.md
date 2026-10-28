# JD-START-003: several beans for one injection point

## What it means

A constructor or field asks for one bean of a type, and the context holds several. Spring cannot
choose, so the application does not start.

## How to confirm

1. The evidence names the injection point and the candidate beans.
2. Check whether each candidate is meant to exist in this environment (profiles, conditions).

## Common causes in Spring applications

- A second implementation of an interface added without marking which one is the default.
- A library that brings its own bean of a common type (`ObjectMapper`, `RestClient`, `DataSource`).

## How to fix

Choose explicitly: mark the default with `@Primary`, inject a specific one with `@Qualifier("name")`,
or inject all of them as a `List<Type>` or `Map<String, Type>` when the code must pick at run time.
