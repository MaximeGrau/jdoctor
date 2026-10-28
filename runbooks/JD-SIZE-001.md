# JD-SIZE-001: max heap too close to the container memory limit

## What it means

The JVM uses memory beyond the heap: metaspace, thread stacks (about 1 MiB per thread), code cache,
direct and mapped buffers (Netty, NIO), GC data structures. When `-Xmx` takes almost all of the
container limit, the kernel kills the container (`OOMKilled`, exit code 137) without any Java error.

## How to confirm

1. `kubectl describe pod <pod>` shows `Last State: Terminated, Reason: OOMKilled`.
2. Start the JVM with `-XX:NativeMemoryTracking=summary` and run `jcmd <pid> VM.native_memory summary`
   to measure the real non-heap usage.

## How to fix

- Prefer `-XX:MaxRAMPercentage=75` to a fixed `-Xmx`: the heap follows the container limit.
- Keep at least 25 % of the limit, and never less than about 300 MiB, outside the heap.
- Limit thread count (`server.tomcat.threads.max`) and direct memory (`-XX:MaxDirectMemorySize`) if they are high.
