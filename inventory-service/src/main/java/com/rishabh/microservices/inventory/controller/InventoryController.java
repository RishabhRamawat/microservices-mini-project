package com.rishabh.microservices.inventory.controller;

import com.rishabh.microservices.inventory.dto.InventoryAdjustmentRequest;
import com.rishabh.microservices.inventory.dto.InventoryRequest;
import com.rishabh.microservices.inventory.dto.InventoryResponse;
import com.rishabh.microservices.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse createInventory(@RequestBody InventoryRequest request) {
        return inventoryService.createInventory(request);
    }

    @GetMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse getInventoryBySku(@PathVariable String sku) {
        return inventoryService.getInventoryBySku(sku);
    }

    @PutMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse updateInventory(@PathVariable String sku, @RequestBody InventoryRequest request) {
        return inventoryService.updateInventory(sku, request);
    }

    // Internal endpoint for Order Service to atomically decrement stock for a single SKU
    @PutMapping("/{sku}/decrement")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse decrementStock(@PathVariable String sku,
                                            @RequestBody InventoryAdjustmentRequest request) {
        return inventoryService.decrementStock(sku, request);
    }
}
