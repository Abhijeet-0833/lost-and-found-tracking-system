package com.lostfound.service;

import com.lostfound.model.Notification;
import com.lostfound.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public boolean markAsRead(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId).map(n -> {
            if (n.getUserId() != null && n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
                return true;
            }
            return false;
        }).orElse(false);
    }
}
