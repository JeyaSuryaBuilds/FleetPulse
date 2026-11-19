package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController extends CrudController<Notification, NotificationRepository> {
    public NotificationController(NotificationRepository repo, CrudDeps deps) {
        super(Notification.class, repo, deps, Permission.NOTIFICATION_READ, Permission.ORG_MANAGE,
                Map.of("userId", "users"), "Notification");
    }
}
