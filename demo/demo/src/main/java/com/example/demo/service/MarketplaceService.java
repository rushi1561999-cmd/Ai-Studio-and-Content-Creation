package com.example.demo.service;

import com.example.demo.dto.MarketplacePublishRequest;
import com.example.demo.entity.MarketplacePost;
import com.example.demo.entity.PostLike;
import com.example.demo.entity.User;
import com.example.demo.repository.MarketplacePostRepository;
import com.example.demo.repository.PostLikeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MarketplaceService {

    private final MarketplacePostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public MarketplaceService(
            MarketplacePostRepository postRepository,
            PostLikeRepository postLikeRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public List<MarketplacePost> getFeed() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public MarketplacePost publish(MarketplacePublishRequest request) {
        if (request.getPromptText() == null || request.getPromptText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt text is required.");
        }

        User user = currentUser();

        MarketplacePost post = new MarketplacePost();
        post.setPromptText(request.getPromptText().trim());
        post.setCategory(
                request.getCategory() != null && !request.getCategory().isBlank()
                        ? request.getCategory()
                        : "Community");
        post.setAuthorName(resolveDisplayName(user));
        post.setLikes(0);

        return postRepository.save(post);
    }

    @Transactional
    public MarketplacePost likePost(String postId) {
        MarketplacePost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found."));

        User user = currentUser();

        if (postLikeRepository.existsByPost_IdAndUser_Id(postId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already liked this post.");
        }

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(user);
        postLikeRepository.save(like);

        post.setLikes(post.getLikes() + 1);
        return postRepository.save(post);
    }

    private User currentUser() {
        String email = workspaceAccessService.currentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private String resolveDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        String email = user.getEmail();
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
