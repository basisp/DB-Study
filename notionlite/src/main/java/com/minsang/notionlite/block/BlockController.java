package com.minsang.notionlite.block;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pages/{pageId}/blocks")
public class BlockController {
    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping
    public List<BlockResponse> getBlocks(@PathVariable Long pageId) {
        return blockService.getBlocks(pageId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlockResponse createBlock(@PathVariable Long pageId, @Valid @RequestBody BlockCreateRequest request) {
        return blockService.createBlock(pageId, request);
    }

    @PatchMapping("/{blockId}")
    public BlockResponse updateBlock(@PathVariable Long pageId,
                                     @PathVariable Long blockId,
                                     @RequestBody BlockUpdateRequest request) {
        return blockService.updateBlock(pageId, blockId, request);
    }

    @DeleteMapping("/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBlock(@PathVariable Long pageId, @PathVariable Long blockId) {
        blockService.deleteBlock(pageId, blockId);
    }

    @PutMapping("/reorder")
    public List<BlockResponse> reorderBlocks(@PathVariable Long pageId,
                                             @Valid @RequestBody BlockReorderRequest request) {
        return blockService.reorderBlocks(pageId, request);
    }
}
