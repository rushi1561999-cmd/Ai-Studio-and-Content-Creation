package com.example.demo.config;

import com.example.demo.entity.AiModel;
import com.example.demo.entity.Role;
import com.example.demo.entity.SubscriptionPlan;
import com.example.demo.enums.AiProvider;
import com.example.demo.repository.AiModelRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.SubscriptionPlanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedReferenceData(
            RoleRepository roleRepository,
            AiModelRepository aiModelRepository,
            SubscriptionPlanRepository subscriptionPlanRepository) {
        return args -> {
            seedRole(roleRepository, "PLATFORM_ADMIN", "Platform Admin", "Full platform access");
            seedRole(roleRepository, "WORKSPACE_OWNER", "Workspace Owner", "Owns and manages a workspace");
            seedRole(roleRepository, "WORKSPACE_EDITOR", "Workspace Editor", "Can edit workspace content");
            seedRole(roleRepository, "WORKSPACE_VIEWER", "Workspace Viewer", "Read-only workspace access");

            seedAiModel(aiModelRepository, "gemini-2.0-flash", "Gemini 2.0 Flash",
                    AiProvider.GEMINI, "TEXT", 1);
            seedAiModel(aiModelRepository, "flux-schnell", "Flux Schnell (Image)",
                    AiProvider.REPLICATE, "IMAGE", 3);
            seedAiModel(aiModelRepository, "zeroscope-v2-xl", "Zeroscope Video",
                    AiProvider.REPLICATE, "VIDEO", 10);
            seedAiModel(aiModelRepository, "gemini-mixed", "Gemini Rich Content",
                    AiProvider.GEMINI, "MIXED", 5);

            seedPlan(subscriptionPlanRepository, "FREE", "Free", 5, 0);
            seedPlan(subscriptionPlanRepository, "STARTER", "Starter", 50, 999);
            seedPlan(subscriptionPlanRepository, "PRO", "Pro", 200, 2999);
        };
    }

    private void seedAiModel(
            AiModelRepository repo,
            String key,
            String displayName,
            AiProvider provider,
            String contentType,
            int credits) {
        repo.findByModelKey(key).ifPresentOrElse(
                existing -> {
                    existing.setContentType(contentType);
                    existing.setCreditsPerUse(credits);
                    repo.save(existing);
                },
                () -> {
                    AiModel model = new AiModel();
                    model.setProvider(provider);
                    model.setModelKey(key);
                    model.setDisplayName(displayName);
                    model.setCreditsPerUse(credits);
                    model.setContentType(contentType);
                    model.setActive(true);
                    repo.save(model);
                }
        );
    }

    private void seedRole(RoleRepository repo, String code, String name, String description) {
        if (!repo.existsByCode(code)) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            repo.save(role);
        }
    }

    private void seedPlan(SubscriptionPlanRepository repo, String code, String name, int credits, int priceCents) {
        if (!repo.findByCode(code).isPresent()) {
            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setCode(code);
            plan.setName(name);
            plan.setMonthlyCredits(credits);
            plan.setPriceCents(priceCents);
            plan.setActive(true);
            repo.save(plan);
        }
    }
}
