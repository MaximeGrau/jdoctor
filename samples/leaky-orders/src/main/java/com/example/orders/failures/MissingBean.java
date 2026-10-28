package com.example.orders.failures;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** BUG (profile missing-bean): nothing implements PaymentGateway. */
public final class MissingBean {

    private MissingBean() {
    }

    public interface PaymentGateway {
        void charge(long cents);
    }

    @Service
    @Profile("missing-bean")
    public static class CheckoutService {
        private final PaymentGateway gateway;

        public CheckoutService(PaymentGateway gateway) {
            this.gateway = gateway;
        }

        public void pay(long cents) {
            gateway.charge(cents);
        }
    }
}
