package dev.jdoctor.cli;

import java.util.ArrayList;
import java.util.List;

/** A JVM with two real problems, used as a target by the end-to-end test: a leak and a deadlock. */
public final class TroubledApp {

    private static final List<byte[]> LEAK = new ArrayList<>();
    private static final Object A = new Object();
    private static final Object B = new Object();

    private TroubledApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        lockInOrder("locker-a", A, B).start();
        lockInOrder("locker-b", B, A).start();
        System.out.println("ready");
        System.out.flush();
        long end = System.currentTimeMillis() + 120_000;
        int tick = 0;
        while (System.currentTimeMillis() < end) {
            LEAK.add(new byte[256 << 10]);
            if (++tick % 5 == 0) {
                // Frequent collections give the leak rule enough after-GC samples in a short recording.
                System.gc();
            }
            Thread.sleep(50);
        }
    }

    private static Thread lockInOrder(String name, Object first, Object second) {
        Thread thread = new Thread(() -> {
            synchronized (first) {
                pause();
                synchronized (second) {
                    System.out.println("unreachable");
                }
            }
        }, name);
        thread.setDaemon(true);
        return thread;
    }

    private static void pause() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
