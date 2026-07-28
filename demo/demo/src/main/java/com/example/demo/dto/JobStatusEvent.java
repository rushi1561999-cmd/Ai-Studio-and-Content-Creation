package com.example.demo.dto;

import com.example.demo.entity.GenerationJob;

public record JobStatusEvent(
        String jobId,
        String workspaceId,
        String status,
        String result,
        String mediaUrl,
        String contentType) {

    public static JobStatusEvent from(GenerationJob job) {
        return new JobStatusEvent(
                job.getId(),
                job.getWorkspaceId(),
                job.getStatus(),
                job.getResult(),
                job.getMediaUrl(),
                job.getContentType());
    }
}
