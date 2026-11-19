package com.fayzo.fleetpulse.domain;

import com.fayzo.fleetpulse.security.Permission;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController extends CrudController<Branch, BranchRepository> {
    public BranchController(BranchRepository repo, CrudDeps deps) {
        super(Branch.class, repo, deps, Permission.DASHBOARD_VIEW, Permission.ORG_MANAGE,
                Map.of(), "Branch");
    }
}
