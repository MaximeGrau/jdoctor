# JD-GC-001: GC pauses take too much time

## What it means

The application is stopped while the collector runs. Above 5 % of the time, users notice latency;
above 15 %, throughput collapses and the JVM may end with `OutOfMemoryError: GC overhead limit exceeded`.

## How to confirm

1. Check the causes in the evidence. `System.gc()` means explicit calls; `Allocation Failure` or
   `G1 Evacuation Pause` means the heap fills up quickly.
2. If JD-MEM-001 is also reported, the heap is filling with live data: treat the leak first.
3. Open `recording.jfr` in JDK Mission Control, "Memory" and "Allocations" pages, to find the code
   that allocates the most.

## How to fix

- Explicit calls: remove them, or add `-XX:+DisableExplicitGC` when a library makes them.
- Heap too small for the live data: raise `-Xmx` or `-XX:MaxRAMPercentage`, within the container limit (see JD-SIZE-001).
- High allocation rate: reuse buffers, avoid building large intermediate collections, stream large results
  instead of loading them (`Stream<T>` repositories, pagination).
- Latency-sensitive services on JDK 21: consider generational ZGC (`-XX:+UseZGC -XX:+ZGenerational`).
