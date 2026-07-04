package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByPost_IdOrderByCreatedAtAsc(String postId);
    void deleteByPost_Id(String postId);

    void deleteByUser_Id(String userId);
}
