package dev.jdoctor.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jdoctor.core.Finding;
import dev.jdoctor.core.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptsTest {

    @Test
    void redactsSecretsAndPersonalData() {
        String text = Redactor.redact("user jane.doe@example.com from 10.1.2.3 with password=hunter2 "
                + "Authorization: Bearer abcdefghijkl url jdbc:postgresql://db.internal:5432/orders");

        assertThat(text).doesNotContain("jane.doe", "10.1.2.3", "hunter2", "abcdefghijkl", "db.internal")
                .contains("<email>", "<ip>", "password=<redacted>", "Bearer <token>", "jdbc:<url>");
    }

    @Test
    void userPromptCarriesEvidenceContextAndRunbook() {
        Finding finding = new Finding("JD-SPR-001", Severity.WARNING, "HikariCP pool saturated",
                List.of("Threads waiting for a connection, per dump: 3, 4, 5"), "Shorten transactions.");

        String prompt = Prompts.user(finding,
                new ExplainContext(List.of("GC: G1"), List.of("JD-MEM-001: heap grows")));

        assertThat(prompt).contains("# Finding JD-SPR-001 (WARNING)", "per dump: 3, 4, 5", "GC: G1",
                "JD-MEM-001: heap grows", "## Runbook", "leak-detection-threshold");
    }
}
