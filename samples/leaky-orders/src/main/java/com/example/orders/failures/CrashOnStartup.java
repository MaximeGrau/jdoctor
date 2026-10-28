package com.example.orders.failures;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** BUG (profile init-crash): reads a setting that was never loaded and dereferences null at startup. */
@Component
@Profile("init-crash")
public class CrashOnStartup {

    private final Map<String, String> settings = Map.of("currency", "EUR");

    @PostConstruct
    void loadDefaults() {
        String region = settings.get("region");
        if (region.isBlank()) {
            throw new IllegalStateException("region must be set");
        }
    }
}
