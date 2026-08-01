package com.lostfound.controller;

import com.lostfound.model.Notification;
import com.lostfound.model.User;
import com.lostfound.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public List<Notification> getUnreadNotifications(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return notificationService.getUnreadNotifications(currentUser.getUserId());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}
