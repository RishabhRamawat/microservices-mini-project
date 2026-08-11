package com.rishabh.microservices.inventory.dto;

public record InventoryResponse(
        String sku,
        Integer quantity
) {
}
