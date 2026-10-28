# JD-EXC-001: recurring exceptions in application code

## What it means

The log holds stack traces of JDK or framework exceptions (such as `NullPointerException`,
`IllegalStateException`, `IndexOutOfBoundsException`) thrown at run time. jdoctor groups them by
type and by the first application frame, so each line of evidence is one bug in one place.

## How to confirm

1. Each line of the evidence gives the count, the type, the application frame and the message.
2. For a `NullPointerException`, the JDK message (helpful NullPointerExceptions, Java 14 and later)
   names the variable or the call that was null.
3. Run `jdoctor analyze` with `--explain --source src/main/java` so the explanation sees the code
   around the frame.

## Common causes in Spring applications

- A repository or cache lookup that returns null for an unknown id.
- An optional request parameter or JSON field that is absent.
- Collections indexed without a size check, `Optional.get()` without a value.

## How to fix

Fix the most frequent first. Decide whether the missing value is legal: if not, fail early with a
clear error (validation, `Optional.orElseThrow` with a business exception); if it is, handle it (a
404 for a missing entity, a default value). Add a test that reproduces the request from the log.
