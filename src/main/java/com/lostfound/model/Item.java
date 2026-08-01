package com.lostfound.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
@Data
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;
    
    private String title;
    private String description;
    private String category;
    private String itemType; // 'LOST' or 'FOUND'
    private String location;
    private String status;   // 'PENDING' or 'MATCHED'
    private String photoUrl; // URL path to the uploaded photo
    private Long reportedBy; // Foreign Key to User ID
    
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
}
}
