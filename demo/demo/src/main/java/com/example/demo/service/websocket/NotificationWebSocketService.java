package com.example.demo.service.websocket;

import com.example.demo.entity.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushToUser(String userId, Notification notification) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
    }
}
