package com.minsang.notionlite.page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PageCreateRequest(
        @NotNull(message = "workspaceId is required")
        Long workspaceId,
        @NotBlank(message = "title is required")
        String title,
        Long parentId
) {
}
