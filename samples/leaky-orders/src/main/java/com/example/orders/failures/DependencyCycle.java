package com.example.orders.failures;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** BUG (profile cycle): StockService and ShippingService need each other. */
public final class DependencyCycle {

    private DependencyCycle() {
    }

    @Service
    @Profile("cycle")
    public static class StockService {
        private final ShippingService shipping;

        public StockService(ShippingService shipping) {
            this.shipping = shipping;
        }

        public ShippingService shipping() {
            return shipping;
        }
    }

    @Service
    @Profile("cycle")
    public static class ShippingService {
        private final StockService stock;

        public ShippingService(StockService stock) {
            this.stock = stock;
        }

        public StockService stock() {
            return stock;
        }
    }
}
