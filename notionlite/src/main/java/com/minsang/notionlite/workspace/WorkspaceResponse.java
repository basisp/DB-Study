package com.minsang.notionlite.workspace;

import com.minsang.notionlite.domain.entity.Workspace;

import java.time.Instant;

public record WorkspaceResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
