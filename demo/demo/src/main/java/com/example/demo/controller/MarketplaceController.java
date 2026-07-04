package com.example.demo.controller;

import com.example.demo.dto.MarketplacePublishRequest;
import com.example.demo.entity.MarketplacePost;
import com.example.demo.service.MarketplaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/feed")
    public ResponseEntity<List<MarketplacePost>> getFeed() {
        return ResponseEntity.ok(marketplaceService.getFeed());
    }

    @PostMapping("/publish")
    public ResponseEntity<MarketplacePost> publishPost(@RequestBody MarketplacePublishRequest request) {
        return ResponseEntity.ok(marketplaceService.publish(request));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<MarketplacePost> likePost(@PathVariable String postId) {
        return ResponseEntity.ok(marketplaceService.likePost(postId));
    }
}
