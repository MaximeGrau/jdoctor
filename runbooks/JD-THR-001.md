# JD-THR-001: deadlock

## What it means

Two or more threads each hold a lock the other needs. They wait forever. Requests that reach them
hang, and the thread pool slowly runs out of threads.

## How to confirm

The JVM detected the deadlock itself; the evidence shows the threads and locks involved. Check that
the same threads appear in several thread dumps.

## How to fix

1. Restart the instance: deadlocked threads never recover.
2. In the code, make every thread take the locks in the same order.
3. Prefer `ReentrantLock.tryLock(timeout)` over `synchronized` where two locks are nested.
4. Look for `synchronized` methods that call other beans: with Spring proxies and callbacks, the lock
   order is easy to break by accident.
