package com.example.demo.service;

import com.example.demo.dto.PromptRequest;
import com.example.demo.dto.PromptResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.Prompt;
import com.example.demo.entity.User;
import com.example.demo.entity.Workspace;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.PromptRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    public PromptService(PromptRepository promptRepository, CategoryRepository categoryRepository,
                         UserRepository userRepository, WorkspaceRepository workspaceRepository) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public PromptResponse createPrompt(PromptRequest request, String userEmail) {
        // 1. Find User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find Workspace
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new RuntimeException("Workspace not found"));

        // 3. Find or Create Category
        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(request.getCategoryName());
                    return categoryRepository.save(newCategory);
                });

        // 4. Create and Save Prompt
        Prompt prompt = new Prompt();
        prompt.setTitle(request.getTitle());
        prompt.setContent(request.getContent());
        prompt.setCategory(category);
        prompt.setWorkspace(workspace);
        prompt.setCreatedBy(user);
        
        Prompt savedPrompt = promptRepository.save(prompt);

        // 5. Build Response
        PromptResponse response = new PromptResponse();
        response.setId(savedPrompt.getId());
        response.setTitle(savedPrompt.getTitle());
        response.setContent(savedPrompt.getContent());
        response.setCategoryName(category.getName());

        return response;
    }
  

    // ... your existing code ...

    public List<PromptResponse> getPromptsByWorkspace(String workspaceId) {
        // Fetch all prompts that belong to this specific workspace
        List<Prompt> prompts = promptRepository.findByWorkspaceId(workspaceId);
        
        // Convert the database entities into clean JSON DTOs
        return prompts.stream().map(prompt -> {
            PromptResponse response = new PromptResponse();
            response.setId(prompt.getId());
            response.setTitle(prompt.getTitle());
            response.setContent(prompt.getContent());
            response.setCategoryName(prompt.getCategory().getName());
            return response;
        }).collect(Collectors.toList());
    }
}