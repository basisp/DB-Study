package com.minsang.notionlite.block;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlockCreateRequest(
        @NotBlank(message = "type is required")
        @Size(max = 50, message = "type must be 50 characters or fewer")
        String type,
        @NotNull(message = "content is required")
        String content,
        Integer positionIndex
) {
}
