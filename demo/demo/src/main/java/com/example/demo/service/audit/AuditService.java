package com.example.demo.service.audit;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String userId, String workspaceId, String action, String entityType, String entityId, String metadata) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setWorkspaceId(workspaceId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setMetadata(metadata);
        auditLogRepository.save(entry);
    }
}
