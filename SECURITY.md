# Security policy

jdoctor attaches to production JVMs and reads their memory statistics, threads and configuration.
Please report vulnerabilities privately to the maintainer through
[GitHub private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
rather than in a public issue.

## What jdoctor sends where

- Collection writes files only to the bundle directory you choose.
- Nothing is sent over the network unless you run with `--explain` or `jdoctor explain` with an LLM.
- In that case only the finding (title, evidence, recommendation), short JVM facts and the runbook
  are sent, after redaction of emails, IP addresses, bearer tokens, JWTs, `password=`-style values
  and JDBC URLs. Raw dumps and recordings are never sent.
- Bundles can contain sensitive data (thread names, class names, system properties in VM.info).
  Treat them like logs.
