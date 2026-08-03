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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public PromptService(PromptRepository promptRepository, CategoryRepository categoryRepository,
                         UserRepository userRepository, WorkspaceRepository workspaceRepository,
                         WorkspaceAccessService workspaceAccessService) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public PromptResponse createPrompt(PromptRequest request, String userEmail) {
        workspaceAccessService.requireWorkspaceAccess(request.getWorkspaceId());
        // 1. Find User
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        // 2. Find Workspace
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found."));

        // 3. Find or Create Category
        String categoryName = request.getCategoryName().trim();
        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(categoryName);
                    return categoryRepository.save(newCategory);
                });

        // 4. Create and Save Prompt
        Prompt prompt = new Prompt();
        prompt.setTitle(request.getTitle().trim());
        prompt.setContent(request.getContent().trim());
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
  

    @Transactional(readOnly = true)
    public Page<PromptResponse> getPromptsByWorkspace(String workspaceId, Pageable pageable) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return promptRepository.findByWorkspace_Id(workspaceId, pageable).map(prompt -> {
            PromptResponse response = new PromptResponse();
            response.setId(prompt.getId());
            response.setTitle(prompt.getTitle());
            response.setContent(prompt.getContent());
            response.setCategoryName(prompt.getCategory().getName());
            return response;
        });
    }
}
