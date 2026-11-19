package com.fayzo.fleetpulse.ai;

import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    public record Request(@NotNull AiContextBuilder.Feature feature, @NotBlank @Size(max = 2000) String question, List<Map<String, String>> history) {}
    private final AiService ai;
    public AiController(AiService ai) { this.ai = ai; }

    @GetMapping("/status")
    public Map<String, Object> status() { Authz.require(Permission.AI_USE); return ai.status(); }

    @PostMapping("/analyze")
    public AiService.Result analyze(@Valid @RequestBody Request r) { return ai.run(r.feature(), r.question(), r.history()); }
}
