package com.rishabh.microservices.order.client;

import com.rishabh.microservices.order.dto.InventoryAdjustmentRequest;
import com.rishabh.microservices.order.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Eureka resolves "inventory-service" at runtime — no hardcoded host or port.
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PutMapping("/api/inventory/{sku}/decrement")
    InventoryResponse decrementStock(@PathVariable("sku") String sku,
                                     @RequestBody InventoryAdjustmentRequest request);
}
