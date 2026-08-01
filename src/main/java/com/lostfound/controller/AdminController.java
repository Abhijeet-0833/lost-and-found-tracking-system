package com.lostfound.controller;

import com.lostfound.model.Match;
import com.lostfound.model.User;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;
import com.lostfound.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.lostfound.repository.ChatRepository chatRepository;

    @Autowired
    private com.lostfound.repository.ItemRepository itemRepository;

    @GetMapping("/matches")
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    @PutMapping("/matches/{id}/approve")
    public String approveChat(@PathVariable Long id) {
        return matchRepository.findById(id).map(match -> {
            match.setStatus("CHAT_APPROVED");
            matchRepository.save(match);
            
            notificationService.createNotification(match.getLostItem().getReportedBy(), "Great news! Admin has approved your match. You can now chat regarding: " + match.getLostItem().getTitle());
            notificationService.createNotification(match.getFoundItem().getReportedBy(), "Great news! Admin has approved your match. You can now chat regarding: " + match.getFoundItem().getTitle());
            
            return "Chat access granted and users notified.";
        }).orElse("Match not found.");
    }

    @PutMapping("/matches/{id}/approve-location")
    public String approveLocationSharing(@PathVariable Long id) {
        return matchRepository.findById(id).map(match -> {
            match.setLocationApproved(true);
            matchRepository.save(match);
            
            notificationService.createNotification(match.getLostItem().getReportedBy(), "Admin has approved Live Location Sharing for your chat regarding: " + match.getLostItem().getTitle());
            notificationService.createNotification(match.getFoundItem().getReportedBy(), "Admin has approved Live Location Sharing for your chat regarding: " + match.getFoundItem().getTitle());
            
            return "Location sharing access granted.";
        }).orElse("Match not found.");
    }

    @PutMapping("/matches/{id}/block-location")
    public String blockLocationSharing(@PathVariable Long id) {
        return matchRepository.findById(id).map(match -> {
            match.setLocationApproved(false);
            matchRepository.save(match);
            
            notificationService.createNotification(match.getLostItem().getReportedBy(), "Admin has disabled Live Location Sharing for your chat regarding: " + match.getLostItem().getTitle());
            notificationService.createNotification(match.getFoundItem().getReportedBy(), "Admin has disabled Live Location Sharing for your chat regarding: " + match.getFoundItem().getTitle());
            
            return "Location sharing access disabled.";
        }).orElse("Match not found.");
    }

    @PutMapping("/matches/{id}/block")
    public String blockChat(@PathVariable Long id) {
        return matchRepository.findById(id).map(match -> {
            match.setStatus("BLOCKED");
            matchRepository.save(match);
            return "Match blocked.";
        }).orElse("Match not found.");
    }

    @PutMapping("/matches/{id}/unblock")
    public String unblockChat(@PathVariable Long id) {
        return matchRepository.findById(id).map(match -> {
            match.setStatus("PENDING_APPROVAL");
            matchRepository.save(match);
            return "Match unblocked and set back to pending.";
        }).orElse("Match not found.");
    }

    @GetMapping("/matches/{id}/chats")
    public ResponseEntity<?> getMatchChats(@PathVariable Long id) {
        // Admin gets absolute power to view any chat
        return ResponseEntity.ok(chatRepository.findByMatch_MatchIdOrderByTimestampAsc(id));
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/users/{id}/block")
    public String blockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            if ("ADMIN".equals(u.getRole())) return "Cannot block an admin.";
            u.setRole("BLOCKED");
            userRepository.save(u);
            return "User blocked.";
        }).orElse("User not found.");
    }

    @PutMapping("/users/{id}/unblock")
    public String unblockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> {
            u.setRole("USER");
            userRepository.save(u);
            return "User unblocked.";
        }).orElse("User not found.");
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        if(userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return "User deleted.";
        }
        return "User not found.";
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Remove any associated matches to prevent foreign key constraint violations
        List<Match> matches = matchRepository.findAll();
        for (Match m : matches) {
            if (m.getLostItem().getItemId().equals(id) || m.getFoundItem().getItemId().equals(id)) {
                // Delete associated chat messages first
                List<com.lostfound.model.ChatMessage> chats = chatRepository.findByMatch_MatchIdOrderByTimestampAsc(m.getMatchId());
                chatRepository.deleteAll(chats);
                matchRepository.delete(m);
            }
        }
        
        itemRepository.deleteById(id);
        return ResponseEntity.ok("Report deleted permanently.");
    }
}
