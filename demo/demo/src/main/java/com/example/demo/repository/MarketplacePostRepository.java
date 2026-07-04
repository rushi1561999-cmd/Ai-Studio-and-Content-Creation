package com.example.demo.repository;

import com.example.demo.entity.MarketplacePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarketplacePostRepository extends JpaRepository<MarketplacePost, String> {
    
    // Automatically fetches all posts and sorts them so the newest are at the top!
    List<MarketplacePost> findAllByOrderByCreatedAtDesc();
}
