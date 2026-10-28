# Contributing to jdoctor

Thanks for helping. The easiest first contribution is a new rule.

## Build

```bash
./gradlew build        # compiles and runs all tests, including an end-to-end test on a live JVM
```

The end-to-end test starts a JVM and attaches to it with `jcmd`, so run it on a machine where
attaching is allowed (Linux or macOS, same user).

## Add a rule

1. Pick an id with the right family prefix (`JD-MEM`, `JD-GC`, `JD-THR`, `JD-SPR`, `JD-SIZE`...) and the next number.
2. Implement `dev.jdoctor.analyze.Rule` in `jdoctor-analyzers/.../rules`. A rule:
   - returns no finding when the data it needs is missing;
   - puts measured numbers in `evidence`, one fact per line;
   - keeps thresholds as named constants.
3. Register it in `Analyzer.withDefaultRules()`.
4. Write `runbooks/<ID>.md`: what it means, how to confirm, common Spring causes, how to fix.
5. Add tests with real data shapes: a thread dump fixture in `src/test/resources/dumps`, or a
   synthetic `JfrSummary`. Cover the case that fires and the one that must not.

## Commits

Sign off every commit (Developer Certificate of Origin): `git commit -s`. No CLA is required.

## Style

Java 21, records for data, no wildcard imports, comments that explain why rather than what.
