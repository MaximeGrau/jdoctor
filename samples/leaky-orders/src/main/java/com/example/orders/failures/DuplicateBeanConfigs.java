package com.example.orders.failures;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** BUG (profile duplicate-bean): both configurations register a bean named priceRounding. */
public final class DuplicateBeanConfigs {

    private DuplicateBeanConfigs() {
    }

    public interface PriceRounding {
        long round(double price);
    }

    @Configuration
    @Profile("duplicate-bean")
    public static class CatalogConfig {
        @Bean
        PriceRounding priceRounding() {
            return Math::round;
        }
    }

    @Configuration
    @Profile("duplicate-bean")
    public static class CheckoutConfig {
        @Bean
        PriceRounding priceRounding() {
            return price -> (long) Math.ceil(price);
        }
    }
}
