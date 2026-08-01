package com.lostfound.repository;

import com.lostfound.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByMatch_MatchIdOrderByTimestampAsc(Long matchId);
}
