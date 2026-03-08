package com.minsang.notionlite.block;

import com.minsang.notionlite.domain.entity.Block;

import java.time.Instant;

public record BlockResponse(
        Long id,
        Long pageId,
        String type,
        String content,
        Integer positionIndex,
        Instant createdAt,
        Instant updatedAt
) {
    public static BlockResponse from(Block block) {
        return new BlockResponse(
                block.getId(),
                block.getPage().getId(),
                block.getType(),
                block.getContent(),
                block.getPositionIndex(),
                block.getCreatedAt(),
                block.getUpdatedAt()
        );
    }
}
