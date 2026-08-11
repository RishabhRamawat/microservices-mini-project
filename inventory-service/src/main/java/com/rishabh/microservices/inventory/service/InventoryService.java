package com.rishabh.microservices.inventory.service;

import com.rishabh.microservices.inventory.dto.InventoryRequest;
import com.rishabh.microservices.inventory.dto.InventoryResponse;
import com.rishabh.microservices.inventory.entity.Inventory;
import com.rishabh.microservices.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.rishabh.microservices.inventory.exception.InventoryNotFoundException;
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryResponse createInventory(InventoryRequest request) {
        if (request.sku() == null || request.sku().isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
        if (request.quantity() == null || request.quantity() < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        // Reject duplicate SKUs early; the DB unique constraint is the final guard
        if (inventoryRepository.findBySku(request.sku()).isPresent()) {
            throw new IllegalStateException("Inventory already exists for SKU: " + request.sku());
        }

        Inventory inventory = new Inventory();
        inventory.setSku(request.sku());
        inventory.setQuantity(request.quantity());

        Inventory saved = inventoryRepository.save(inventory);
        return toInventoryResponse(saved);
    }

    public InventoryResponse getInventoryBySku(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for SKU: " + sku));

        return toInventoryResponse(inventory);
    }

    public InventoryResponse updateInventory(String sku, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Inventory not found for SKU: " + sku));

        if (request.quantity() == null || request.quantity() < 0) {
            throw new IllegalArgumentException("Quantity must be provided and non negative");
        }

        inventory.setQuantity(request.quantity());

        Inventory saved = inventoryRepository.save(inventory);
        return toInventoryResponse(saved);
    }

    private InventoryResponse toInventoryResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getSku(),
                inventory.getQuantity()
        );
    }
}
