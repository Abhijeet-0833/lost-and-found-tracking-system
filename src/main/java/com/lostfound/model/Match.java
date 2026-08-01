package com.lostfound.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_matches")
@Data
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    @ManyToOne
    @JoinColumn(name = "lost_item_id")
    private Item lostItem;

    @ManyToOne
    @JoinColumn(name = "found_item_id")
    private Item foundItem;

    // e.g., PENDING_APPROVAL, CHAT_APPROVED, BLOCKED
    private String status; 
    
    @Column(name = "location_approved")
    private Boolean locationApproved = false;

    private LocalDateTime matchDate;

    @PrePersist
    protected void onCreate() {
        this.matchDate = LocalDateTime.now();
    }
}
