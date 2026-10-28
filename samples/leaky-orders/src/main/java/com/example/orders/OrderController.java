package com.example.orders;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderCache cache;
    private final ReportService reports;

    public OrderController(OrderCache cache, ReportService reports) {
        this.cache = cache;
        this.reports = reports;
    }

    @PostMapping("/orders")
    public Map<String, Object> create(@RequestParam(defaultValue = "256") int sizeKb) {
        UUID id = cache.remember(sizeKb);
        return Map.of("id", id, "cached", cache.size());
    }

    @GetMapping("/reports/monthly")
    public Map<String, Object> report() throws InterruptedException {
        return Map.of("tables", reports.monthlyReport());
    }
}
