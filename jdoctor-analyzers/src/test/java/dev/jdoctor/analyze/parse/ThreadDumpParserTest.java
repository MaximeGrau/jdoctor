package dev.jdoctor.analyze.parse;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.analyze.Fixtures;
import org.junit.jupiter.api.Test;

class ThreadDumpParserTest {

    @Test
    void parsesThreadsStatesAndFrames() {
        ThreadDump dump = ThreadDumpParser.parse(Fixtures.resource("dumps/hikari-deadlock.txt"));

        assertThat(dump.threads()).extracting(ThreadDump.ThreadInfo::name).containsExactly(
                "http-nio-8080-exec-1", "http-nio-8080-exec-2", "http-nio-8080-exec-3",
                "locker-a", "locker-b", "VM Thread", "GC Thread#0");
        ThreadDump.ThreadInfo first = dump.threads().get(0);
        assertThat(first.state()).isEqualTo("TIMED_WAITING");
        assertThat(first.frames()).first().asString().startsWith("jdk.internal.misc.Unsafe.park");
        assertThat(first.hasFrameStartingWith("com.zaxxer.hikari.pool.HikariPool.getConnection")).isTrue();
        assertThat(dump.threads().get(5).state()).isEqualTo("UNKNOWN");
    }

    @Test
    void extractsDeadlockReportWithoutTreatingItAsThreads() {
        ThreadDump dump = ThreadDumpParser.parse(Fixtures.resource("dumps/hikari-deadlock.txt"));

        assertThat(dump.hasDeadlock()).isTrue();
        assertThat(dump.deadlock()).startsWith("Found one Java-level deadlock").endsWith("Found 1 deadlock.")
                .contains("which is held by \"locker-b\"");
    }

    @Test
    void noDeadlockInIdleDump() {
        ThreadDump dump = ThreadDumpParser.parse(Fixtures.resource("dumps/idle.txt"));

        assertThat(dump.hasDeadlock()).isFalse();
        assertThat(dump.threads()).hasSize(2);
    }
}
