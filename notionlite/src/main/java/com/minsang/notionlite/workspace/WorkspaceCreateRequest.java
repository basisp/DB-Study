package com.minsang.notionlite.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(max = 200, message = "Workspace name must be 200 characters or fewer")
        String name
) {
}
