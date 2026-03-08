package com.minsang.notionlite.page;

import com.minsang.notionlite.domain.entity.Page;

import java.time.Instant;

public record PageResponse(
        Long id,
        Long workspaceId,
        String title,
        Long parentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PageResponse from(Page page) {
        Long parentId = page.getParent() == null ? null : page.getParent().getId();
        return new PageResponse(
                page.getId(),
                page.getWorkspace().getId(),
                page.getTitle(),
                parentId,
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }
}
