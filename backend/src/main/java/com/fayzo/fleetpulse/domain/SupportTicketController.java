package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/support-tickets")
public class SupportTicketController extends CrudController<SupportTicket, SupportTicketRepository> {
    public SupportTicketController(SupportTicketRepository repo, CrudDeps deps) {
        super(SupportTicket.class, repo, deps, Permission.SUPPORT_MANAGE, Permission.SUPPORT_MANAGE,
                Map.of("createdByUserId", "users"), "SupportTicket");
    }
}
