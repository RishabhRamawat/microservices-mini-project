package com.rishabh.microservices.inventory.repository;

import com.rishabh.microservices.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Derived query; avoids a manual @Query for a simple lookup by unique field
    Optional<Inventory> findBySku(String sku);
}
