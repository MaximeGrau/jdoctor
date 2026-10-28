package com.example.orders;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** BUG: entries are never evicted, so every order stays in memory forever (JD-MEM-001). */
@Component
public class OrderCache {

    private static final Map<UUID, byte[]> ORDERS = new ConcurrentHashMap<>();

    public UUID remember(int sizeKb) {
        UUID id = UUID.randomUUID();
        ORDERS.put(id, new byte[sizeKb * 1024]);
        return id;
    }

    public byte[] find(UUID id) {
        return ORDERS.get(id);
    }

    public int size() {
        return ORDERS.size();
    }
}
