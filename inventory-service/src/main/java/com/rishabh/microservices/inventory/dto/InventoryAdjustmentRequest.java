package com.rishabh.microservices.inventory.dto;

// Keeps decrement semantics separate from absolute inventory updates.
public record InventoryAdjustmentRequest(
        Integer quantity
) {
}
