package com.lostfound.controller;

import com.lostfound.model.ChatMessage;
import com.lostfound.model.Match;
import com.lostfound.model.User;
import com.lostfound.repository.ChatRepository;
import com.lostfound.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MatchRepository matchRepository;

    @GetMapping("/matches")
    public List<Match> getUserMatches(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return matchRepository.findMatchesForUser(currentUser.getUserId());
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<?> getMessages(@PathVariable Long matchId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Match match = matchRepository.findById(matchId).orElse(null);
        
        if(match == null) return ResponseEntity.notFound().build();
        if(!match.getStatus().equals("CHAT_APPROVED")) return ResponseEntity.status(403).body("Chat not approved");

        long currentId = currentUser.getUserId().longValue();
        long lostReporter = match.getLostItem().getReportedBy().longValue();
        long foundReporter = match.getFoundItem().getReportedBy().longValue();

        if (lostReporter != currentId && foundReporter != currentId) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        return ResponseEntity.ok(chatRepository.findByMatch_MatchIdOrderByTimestampAsc(matchId));
    }

    @PostMapping("/{matchId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long matchId, @RequestBody Map<String, String> payload, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Match match = matchRepository.findById(matchId).orElse(null);
        
        if(match == null) return ResponseEntity.notFound().build();
        if(!match.getStatus().equals("CHAT_APPROVED")) return ResponseEntity.status(403).body("Chat not approved");

        long currentId = currentUser.getUserId().longValue();
        long lostReporter = match.getLostItem().getReportedBy().longValue();
        long foundReporter = match.getFoundItem().getReportedBy().longValue();

        if (lostReporter != currentId && foundReporter != currentId) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        Long receiverId = (lostReporter == currentId) ? match.getFoundItem().getReportedBy() : match.getLostItem().getReportedBy();

        ChatMessage message = new ChatMessage();
        message.setMatch(match);
        message.setSenderId(currentUser.getUserId());
        message.setReceiverId(receiverId);
        message.setContent(payload.get("content"));

        return ResponseEntity.ok(chatRepository.save(message));
    }
}
