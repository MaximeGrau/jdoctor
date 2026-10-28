package com.example.orders.failures;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** BUG (profile two-candidates): InvoiceService needs one TaxPolicy but two exist. */
public final class TwoCandidates {

    private TwoCandidates() {
    }

    public interface TaxPolicy {
        double rate();
    }

    @Configuration
    @Profile("two-candidates")
    public static class TaxConfig {
        @Bean
        TaxPolicy frenchTax() {
            return () -> 0.20;
        }

        @Bean
        TaxPolicy germanTax() {
            return () -> 0.19;
        }
    }

    @Service
    @Profile("two-candidates")
    public static class InvoiceService {
        private final TaxPolicy taxPolicy;

        public InvoiceService(TaxPolicy taxPolicy) {
            this.taxPolicy = taxPolicy;
        }

        public double withTax(double amount) {
            return amount * (1 + taxPolicy.rate());
        }
    }
}
