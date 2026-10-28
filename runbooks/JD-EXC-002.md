# JD-EXC-002: application exceptions escape unhandled

## What it means

Exception classes defined by the application reach the framework without a handler. Spring logs
them as errors with a full stack trace and answers HTTP 500, even when the case is an expected
business situation such as an unknown id.

## How to confirm

1. The evidence lists each exception class, how often it was thrown and where.
2. Call the endpoint with the same input: the response is a 500 instead of a 4xx.

## Common causes in Spring applications

- A `NotFoundException` or `ValidationException` without `@ResponseStatus`.
- A `@RestControllerAdvice` that exists but does not cover this exception type, or sits in a package that is not scanned.

## How to fix

Map each business exception to a status: `@ResponseStatus(HttpStatus.NOT_FOUND)` on the class, or
an `@ExceptionHandler` in a `@RestControllerAdvice` that returns a `ProblemDetail`
(`spring.mvc.problemdetails.enabled=true` turns on RFC 9457 responses for Spring's own exceptions).
Log expected cases at INFO or WARN without a stack trace, so real errors stand out.
