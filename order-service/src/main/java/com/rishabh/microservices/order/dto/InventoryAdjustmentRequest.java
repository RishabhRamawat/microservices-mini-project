package com.rishabh.microservices.order.dto;

// Local mirror of the Inventory Service DTO; avoids cross-service class coupling.
public record InventoryAdjustmentRequest(
        Integer quantity
) {
}
