package com.lostfound.service;

import com.lostfound.model.Item;
import com.lostfound.model.Match;
import com.lostfound.model.User;
import com.lostfound.repository.ItemRepository;
import com.lostfound.repository.MatchRepository;
import com.lostfound.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JavaMailSender mailSender;

    public List<Item> findPotentialMatches(Item newItem) {
        String lookForType = newItem.getItemType().equalsIgnoreCase("FOUND") ? "LOST" : "FOUND";
        
        List<Item> candidates = itemRepository.findByCategoryIgnoreCase(newItem.getCategory());

        return candidates.stream()
            .filter(item -> item.getItemType().equalsIgnoreCase(lookForType))
            .filter(item -> item.getLocation().toLowerCase().contains(newItem.getLocation().toLowerCase()) || 
                            newItem.getLocation().toLowerCase().contains(item.getLocation().toLowerCase()))
            .collect(Collectors.toList());
    }

    public void notifyMatches(Item newItem, List<Item> matches) {
        if (matches == null || matches.isEmpty()) return;

        User reporter = userRepository.findById(newItem.getReportedBy()).orElse(null);
        if (reporter == null) return;

        for (Item matchedItem : matches) {
            User owner = userRepository.findById(matchedItem.getReportedBy()).orElse(null);
            if (owner != null) {
                // Determine which is Lost and which is Found
                Item lostItem = newItem.getItemType().equalsIgnoreCase("LOST") ? newItem : matchedItem;
                Item foundItem = newItem.getItemType().equalsIgnoreCase("FOUND") ? newItem : matchedItem;

                // Check if match already exists
                Match existingMatch = matchRepository.findByItems(lostItem.getItemId(), foundItem.getItemId());
                if (existingMatch == null) {
                    Match match = new Match();
                    match.setLostItem(lostItem);
                    match.setFoundItem(foundItem);
                    match.setStatus("PENDING_APPROVAL");
                    matchRepository.save(match);
                }

                // Send Notification & Email
                notificationService.createNotification(reporter.getUserId(), "A potential match was found for your item: " + newItem.getTitle() + ". Waiting for admin verification.");
                sendMatchEmail(reporter.getEmail(), newItem, matchedItem);
                
                notificationService.createNotification(owner.getUserId(), "A potential match was found for your item: " + matchedItem.getTitle() + ". Waiting for admin verification.");
                sendMatchEmail(owner.getEmail(), matchedItem, newItem);
            }
        }
    }

    private void sendMatchEmail(String toEmail, Item theirItem, Item matchedItem) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Match Found for your Item: " + theirItem.getTitle());
            message.setText("Good news!\n\nWe found a potential match for your " + theirItem.getItemType() + " item: " + theirItem.getTitle() + ".\n\n" +
                    "Matched Item Details:\n" +
                    "Title: " + matchedItem.getTitle() + "\n" +
                    "Location: " + matchedItem.getLocation() + "\n\n" +
                    "The match has been sent to our Admins for verification. You will be notified in the app once you are approved to chat with the other party!\n\n" +
                    "Regards,\nCampus Tracker Team");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send match email to " + toEmail + ": " + e.getMessage());
        }
    }
}