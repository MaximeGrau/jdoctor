# JD-MEM-001: heap after GC grows steadily

## What it means

After each garbage collection, the heap still in use is the live data plus what the collector
did not reach yet. jdoctor keeps the lowest value of each part of the recording (the floor) and
fits a line through them. A floor that keeps rising means objects stay reachable and pile up:
a memory leak, or a cache with no limit.

## How to confirm

1. Run `jdoctor analyze` again 10 to 30 minutes later. A leak keeps the same slope.
2. Compare two heap histograms a few minutes apart: `jcmd <pid> GC.class_histogram`.
   Classes whose instance count only grows are the suspects.
3. Take a heap dump (`jcmd <pid> GC.heap_dump /tmp/heap.hprof`) and open it in Eclipse MAT.
   The "Leak Suspects" report shows what retains the memory. A heap dump pauses the JVM and
   can be as large as the heap: do it on one instance, out of the load balancer if possible.

## Common causes in Spring applications

- A `static` `Map` or `List` used as a cache, with no eviction.
- `@Cacheable` with a cache provider configured without `maximumSize` or TTL.
- `ThreadLocal` values never removed in pooled threads.
- Listeners, subscriptions or scheduled tasks registered per request and never removed.
- Hibernate first-level cache growing in a long `@Transactional` batch: flush and clear every N entities.

## How to fix

Bound every cache (Caffeine with `maximumSize` and `expireAfterWrite`), remove `ThreadLocal`
values in a `finally` block, and unregister listeners. Raising `-Xmx` only delays the crash.
