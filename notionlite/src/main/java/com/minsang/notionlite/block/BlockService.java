package com.minsang.notionlite.block;

import com.minsang.notionlite.common.BadRequestException;
import com.minsang.notionlite.common.ResourceNotFoundException;
import com.minsang.notionlite.domain.entity.Block;
import com.minsang.notionlite.domain.entity.Page;
import com.minsang.notionlite.domain.repository.BlockRepository;
import com.minsang.notionlite.domain.repository.PageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
public class BlockService {
    private final BlockRepository blockRepository;
    private final PageRepository pageRepository;

    public BlockService(BlockRepository blockRepository, PageRepository pageRepository) {
        this.blockRepository = blockRepository;
        this.pageRepository = pageRepository;
    }

    public List<BlockResponse> getBlocks(Long pageId) {
        ensurePageExists(pageId);
        return blockRepository.findByPageIdOrderByPositionIndexAsc(pageId)
                .stream()
                .map(BlockResponse::from)
                .toList();
    }

    public BlockResponse createBlock(Long pageId, BlockCreateRequest request) {
        Page page = ensurePageExists(pageId);
        List<Block> blocks = blockRepository.findByPageIdOrderByPositionIndexAsc(pageId);

        int targetIndex = request.positionIndex() == null ? blocks.size() : request.positionIndex();
        if (targetIndex < 0 || targetIndex > blocks.size()) {
            throw new BadRequestException("positionIndex out of range");
        }

        shiftPositions(blocks, targetIndex, +1);

        Block block = new Block();
        block.setPage(page);
        block.setType(request.type().trim());
        block.setContent(request.content());
        block.setPositionIndex(targetIndex);

        Block saved = blockRepository.save(block);
        return BlockResponse.from(saved);
    }

    public BlockResponse updateBlock(Long pageId, Long blockId, BlockUpdateRequest request) {
        if (!request.hasAnyField()) {
            throw new BadRequestException("At least one field is required");
        }

        Block block = blockRepository.findByIdAndPageId(blockId, pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Block not found"));

        if (request.getType() != null) {
            String type = request.getType().trim();
            if (type.isEmpty()) {
                throw new BadRequestException("type cannot be blank");
            }
            block.setType(type);
        }

        if (request.getContent() != null) {
            block.setContent(request.getContent());
        }

        if (request.getPositionIndex() != null && !Objects.equals(request.getPositionIndex(), block.getPositionIndex())) {
            moveBlock(pageId, blockId, request.getPositionIndex());
            block = blockRepository.findByIdAndPageId(blockId, pageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Block not found"));
        }

        return BlockResponse.from(blockRepository.save(block));
    }

    public void deleteBlock(Long pageId, Long blockId) {
        Block block = blockRepository.findByIdAndPageId(blockId, pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Block not found"));

        blockRepository.delete(block);
        blockRepository.flush();

        List<Block> remainingBlocks = blockRepository.findByPageIdOrderByPositionIndexAsc(pageId);
        Map<Long, Block> byId = new HashMap<>();
        List<Long> orderedIds = new ArrayList<>();
        for (Block remaining : remainingBlocks) {
            byId.put(remaining.getId(), remaining);
            orderedIds.add(remaining.getId());
        }
        applyPositionMap(byId, orderedIds);
    }

    public List<BlockResponse> reorderBlocks(Long pageId, BlockReorderRequest request) {
        ensurePageExists(pageId);

        List<Block> blocks = blockRepository.findByPageIdOrderByPositionIndexAsc(pageId);
        List<Long> submittedIds = request.blockIds() == null ? List.of() : request.blockIds();

        Set<Long> currentIds = new HashSet<>(blocks.stream().map(Block::getId).toList());
        Set<Long> nextIds = new HashSet<>(submittedIds);

        if (currentIds.size() != nextIds.size() || !currentIds.equals(nextIds)) {
            throw new BadRequestException("blockIds must include all blocks for the page exactly once");
        }

        Map<Long, Block> byId = new HashMap<>();
        for (Block block : blocks) {
            byId.put(block.getId(), block);
        }

        applyPositionMap(byId, submittedIds);

        return blockRepository.findByPageIdOrderByPositionIndexAsc(pageId)
                .stream()
                .map(BlockResponse::from)
                .toList();
    }

    private Page ensurePageExists(Long pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
    }

    private void shiftPositions(List<Block> blocks, int startIndex, int delta) {
        List<Block> affected = blocks.stream()
                .filter(block -> block.getPositionIndex() >= startIndex)
                .sorted(Comparator.comparingInt(Block::getPositionIndex))
                .toList();

        if (affected.isEmpty()) {
            return;
        }

        Map<Long, Integer> originalIndexById = new HashMap<>();
        for (Block block : affected) {
            originalIndexById.put(block.getId(), block.getPositionIndex());
        }

        int tempOffset = affected.size() + 1000;
        for (int index = 0; index < affected.size(); index++) {
            Block block = affected.get(index);
            block.setPositionIndex(-(tempOffset + index));
        }
        blockRepository.saveAll(affected);
        blockRepository.flush();

        for (Block block : affected) {
            int originalIndex = originalIndexById.get(block.getId());
            block.setPositionIndex(originalIndex + delta);
        }
        blockRepository.saveAll(affected);
        blockRepository.flush();
    }

    private void moveBlock(Long pageId, Long blockId, int targetIndex) {
        List<Block> blocks = blockRepository.findByPageIdOrderByPositionIndexAsc(pageId);
        int size = blocks.size();

        if (targetIndex < 0 || targetIndex >= size) {
            throw new BadRequestException("positionIndex out of range");
        }

        List<Long> ids = new ArrayList<>(blocks.stream().map(Block::getId).toList());
        ids.remove(blockId);
        ids.add(targetIndex, blockId);

        Map<Long, Block> byId = new HashMap<>();
        for (Block block : blocks) {
            byId.put(block.getId(), block);
        }
        applyPositionMap(byId, ids);
    }

    private void applyPositionMap(Map<Long, Block> byId, List<Long> orderedIds) {
        List<Block> allBlocks = new ArrayList<>(byId.values());
        int tempOffset = orderedIds.size() + 1000;
        for (int index = 0; index < orderedIds.size(); index++) {
            Block block = byId.get(orderedIds.get(index));
            block.setPositionIndex(-(tempOffset + index));
        }
        blockRepository.saveAll(allBlocks);
        blockRepository.flush();
        for (int index = 0; index < orderedIds.size(); index++) {
            Block block = byId.get(orderedIds.get(index));
            block.setPositionIndex(index);
        }
        blockRepository.saveAll(allBlocks);
        blockRepository.flush();
    }
}
