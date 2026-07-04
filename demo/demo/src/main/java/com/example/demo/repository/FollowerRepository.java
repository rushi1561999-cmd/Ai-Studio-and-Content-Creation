package com.example.demo.repository;

import com.example.demo.entity.Follower;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowerRepository extends JpaRepository<Follower, String> {
    boolean existsByFollower_IdAndFollowing_Id(String followerId, String followingId);
    List<Follower> findByFollowing_Id(String followingId);

    void deleteByFollower_Id(String userId);

    void deleteByFollowing_Id(String userId);
}
