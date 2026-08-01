package com.lostfound.controller;

import com.lostfound.model.Item;
import com.lostfound.model.User; // Changed 'user' to 'User'
import com.lostfound.repository.ItemRepository;
import com.lostfound.service.MatchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*") // Crucial if you have a separate frontend (React/LiveServer)
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MatchService matchService;

    // 1. Get all items to display on the feed
    @GetMapping
    public List<Item> getAllItems(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        if ("ADMIN".equals(currentUser.getRole())) {
            return itemRepository.findAllByOrderByCreatedAtDesc();
        }
        return itemRepository.findByReportedByOrderByCreatedAtDesc(currentUser.getUserId());
    }

    // 2. Report a new item
    @PostMapping(value = "/report", consumes = {"multipart/form-data"})
    public ResponseEntity<?> reportItem(
            @ModelAttribute Item item, 
            @RequestParam(value = "photo", required = false) org.springframework.web.multipart.MultipartFile photo,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        
        // Prevent duplicate submissions
        List<Item> existingItems = itemRepository.findByReportedByOrderByCreatedAtDesc(currentUser.getUserId());
        for (Item existing : existingItems) {
            if (existing.getTitle().equalsIgnoreCase(item.getTitle()) &&
                existing.getItemType().equalsIgnoreCase(item.getItemType()) &&
                existing.getDescription().equalsIgnoreCase(item.getDescription())) {
                return ResponseEntity.badRequest().body("Report already submitted");
            }
        }
        
        item.setReportedBy(currentUser.getUserId());
        item.setStatus("PENDING");

        // Handle photo upload
        if (photo != null && !photo.isEmpty()) {
            try {
                String uploadsDir = "uploads/";
                java.io.File dir = new java.io.File(uploadsDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String originalFilename = photo.getOriginalFilename();
                // Replace spaces and special chars in filename just in case
                if (originalFilename != null) {
                    originalFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                }
                String filename = System.currentTimeMillis() + "_" + originalFilename;
                java.io.File serverFile = new java.io.File(dir.getAbsolutePath() + java.io.File.separator + filename);
                photo.transferTo(serverFile);
                item.setPhotoUrl("/uploads/" + filename);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Failed to upload photo");
            }
        }

        Item savedItem = itemRepository.save(item);
    
        // Find matches immediately
        List<Item> matches = matchService.findPotentialMatches(savedItem);
        
        if (matches.isEmpty()) {
            return ResponseEntity.ok("Item reported. No immediate matches found.");
        } else {
            // Trigger email notifications to both parties
            matchService.notifyMatches(savedItem, matches);
            return ResponseEntity.ok(matches); // Return the list of potential items to the user!
        }
    }

    @GetMapping("/search")
    public List<Item> searchItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type) {

        if (category != null && location != null) {
            return itemRepository.findByCategoryAndLocationContainingIgnoreCase(category, location);
        } else if (category != null) {
            return itemRepository.findByCategoryIgnoreCase(category);
        } else if (location != null) {
            return itemRepository.findByLocationContainingIgnoreCase(location);
        } else if (type != null) {
            return itemRepository.findByItemType(type);
        }
        return itemRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/{id}/run-match")
    public ResponseEntity<?> runMatching(@PathVariable Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Item item = itemRepository.findById(id).orElse(null);

        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        if (!item.getReportedBy().equals(currentUser.getUserId())) {
            return ResponseEntity.status(403).body("You can only run matching on your own items.");
        }

        List<Item> matches = matchService.findPotentialMatches(item);
        
        if (matches.isEmpty()) {
            return ResponseEntity.ok("No new matches found at this time.");
        } else {
            // This will safely ignore existing matches and only create/notify for NEW matches
            matchService.notifyMatches(item, matches);
            return ResponseEntity.ok("Found " + matches.size() + " potential match(es)! Sent to Admin for approval.");
        }
    }
    @PutMapping("/{id}/status")
public ResponseEntity<Item> updateStatus(@PathVariable Long id, @RequestParam String status) {
    return itemRepository.findById(id).map(item -> {
        item.setStatus(status);
        return ResponseEntity.ok(itemRepository.save(item));
    }).orElse(ResponseEntity.notFound().build());
}

}