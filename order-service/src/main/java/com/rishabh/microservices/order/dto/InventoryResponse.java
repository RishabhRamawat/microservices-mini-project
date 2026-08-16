package com.rishabh.microservices.order.dto;

// Local mirror for deserializing Inventory Service responses via Feign; avoids cross-service coupling.
public record InventoryResponse(
        String sku,
        Integer quantity
) {
}
