package com.example.demo.repository;

import com.example.demo.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {

    boolean existsByPost_IdAndUser_Id(String postId, String userId);
    void deleteByPost_Id(String postId);

    void deleteByUser_Id(String userId);
}
