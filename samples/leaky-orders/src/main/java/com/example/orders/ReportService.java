package com.example.orders;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** BUG: the slow "remote call" runs inside the transaction and keeps the connection (JD-SPR-001). */
    @Transactional(readOnly = true)
    public long monthlyReport() throws InterruptedException {
        Long count = jdbc.queryForObject("select count(*) from information_schema.tables", Long.class);
        Thread.sleep(3_000);
        return count == null ? 0 : count;
    }
}
