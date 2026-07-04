package com.example.demo.service.notification;

import com.example.demo.entity.Notification;
import com.example.demo.entity.User;
import com.example.demo.enums.NotificationType;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.websocket.NotificationWebSocketService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketService webSocketService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationWebSocketService webSocketService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }

    @Transactional
    public Notification notifyUser(String userId, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        Notification saved = notificationRepository.save(notification);
        webSocketService.pushToUser(userId, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> listForEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Notification markRead(String notificationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."));
        System.out.println("Marking notification as read - User ID: " + user.getId() + ", Notification User ID: " + notification.getUserId());
        if (!notification.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification.");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        System.out.println("Marking all notifications as read for user: " + user.getId());
        return notificationRepository.markAllAsReadByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }
}
