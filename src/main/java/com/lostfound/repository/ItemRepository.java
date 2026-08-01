package com.lostfound.repository;


import com.lostfound.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // Spring Boot automatically creates the SQL query from this method name!
    List<Item> findByItemTypeAndTitleContainingIgnoreCase(String itemType, String title);
    
    // For the Admin Dashboard
    List<Item> findAllByOrderByCreatedAtDesc();

    // For the User Dashboard
    List<Item> findByReportedByOrderByCreatedAtDesc(Long reportedBy);

    // Find by Category (e.g., "Electronics")
    List<Item> findByCategoryIgnoreCase(String category);

    // Find by Location (e.g., "Library")
    List<Item> findByLocationContainingIgnoreCase(String location);

    // Find by Type (e.g., "LOST" or "FOUND")
    List<Item> findByItemType(String itemType);

    // Combined search: find items by category AND location
    List<Item> findByCategoryAndLocationContainingIgnoreCase(String category, String location);
    
}
