package com.rishabh.microservices.inventory.dto;

public record InventoryRequest(
        String sku,
        Integer quantity
) {
}
