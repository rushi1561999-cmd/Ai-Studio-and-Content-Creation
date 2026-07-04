package com.example.demo.controller;

import com.example.demo.entity.Notification;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final WorkspaceAccessService workspaceAccessService;

    public NotificationController(
            NotificationService notificationService,
            WorkspaceAccessService workspaceAccessService) {
        this.notificationService = notificationService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list() {
        String email = workspaceAccessService.currentUserEmail();
        return ResponseEntity.ok(notificationService.listForEmail(email));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        String email = workspaceAccessService.currentUserEmail();
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(email)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable String id) {
        String email = workspaceAccessService.currentUserEmail();
        System.out.println("Marking notification " + id + " as read for user: " + email);
        return ResponseEntity.ok(notificationService.markRead(id, email));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        String email = workspaceAccessService.currentUserEmail();
        System.out.println("Marking all notifications as read for user: " + email);
        int updated = notificationService.markAllAsRead(email);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
