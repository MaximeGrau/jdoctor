# JD-START-006: port already in use

## What it means

The embedded web server could not bind its port because another process listens on it.

## How to confirm

1. The evidence gives the port.
2. Find the process: `ss -ltnp | grep :8080` on Linux, `lsof -i :8080` on macOS.

## Common causes

- A previous instance of the application still running, often after a restart from the IDE.
- Two services on the same host configured with the same `server.port`.
- In a container, a port mapping that clashes with another container.

## How to fix

Stop the other process, or change `server.port` (`SERVER_PORT` in the environment). In tests, use
`server.port=0` or `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
