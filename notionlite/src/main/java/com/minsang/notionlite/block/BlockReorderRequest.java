package com.minsang.notionlite.block;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BlockReorderRequest(
        @NotNull(message = "blockIds is required")
        List<Long> blockIds
) {
}
