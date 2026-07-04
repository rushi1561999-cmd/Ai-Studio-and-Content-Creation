package com.example.demo.service.storage;

import com.example.demo.entity.Folder;
import com.example.demo.repository.FolderRepository;
import com.example.demo.service.WorkspaceAccessService;
import com.example.demo.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Folder folder = new Folder();
        folder.setWorkspaceId(workspaceId);
        folder.setName(name);
        folder.setParentId(parentId);
        Folder saved = folderRepository.save(folder);

        auditService.log(null, workspaceId, "FOLDER_CREATED", "Folder", saved.getId(), null);
        return saved;
    }
}
