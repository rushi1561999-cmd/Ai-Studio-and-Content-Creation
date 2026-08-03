package com.example.demo.service.storage;

import com.example.demo.entity.Folder;
import com.example.demo.repository.FolderRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final AuditService auditService;

    public FolderService(
            FolderRepository folderRepository,
            WorkspaceAccessService workspaceAccessService,
            AuditService auditService) {
        this.folderRepository = folderRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Folder> listFolders(String workspaceId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);
        return folderRepository.findByWorkspaceIdOrderByNameAsc(workspaceId);
    }

    @Transactional
    public Folder createFolder(String workspaceId, String name, String parentId) {
        workspaceAccessService.requireWorkspaceAccess(workspaceId);

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank() || cleanName.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must contain 1 to 120 characters.");
        }
        String cleanParentId = parentId == null || parentId.isBlank() ? null : parentId;
        if (cleanParentId != null) {
            Folder parent = folderRepository.findById(cleanParentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found."));
            if (!workspaceId.equals(parent.getWorkspaceId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent folder belongs to another workspace.");
            }
        }
        if (folderRepository.existsByWorkspaceIdAndParentIdAndNameIgnoreCase(
                workspaceId, cleanParentId, cleanName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder with this name already exists here.");
        }

        Folder folder = new Folder();
        folder.setWorkspaceId(workspaceId);
        folder.setName(cleanName);
        folder.setParentId(cleanParentId);
        Folder saved = folderRepository.save(folder);

        auditService.log(null, workspaceId, "FOLDER_CREATED", "Folder", saved.getId(), null);
        return saved;
    }
}
