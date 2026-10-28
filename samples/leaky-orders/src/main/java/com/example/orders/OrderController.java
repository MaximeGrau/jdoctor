package com.example.orders;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/orders/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        byte[] order = cache.find(id);
        if (order == null) {
            throw new OrderNotFoundException(id);
        }
        return Map.of("id", id, "bytes", order.length);
    }

    /** BUG: an unknown id returns null from the cache and the length call throws a NullPointerException. */
    @GetMapping("/orders/{id}/size")
    public Map<String, Object> size(@PathVariable UUID id) {
        byte[] order = cache.find(id);
        return Map.of("bytes", order.length);
    }

    @GetMapping("/reports/monthly")
    public Map<String, Object> report() throws InterruptedException {
        return Map.of("tables", reports.monthlyReport());
    }
}
