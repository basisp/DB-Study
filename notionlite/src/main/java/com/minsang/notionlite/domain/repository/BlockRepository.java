package com.minsang.notionlite.domain.repository;

import com.minsang.notionlite.domain.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByPageIdOrderByPositionIndexAsc(Long pageId);

    Optional<Block> findByIdAndPageId(Long id, Long pageId);
}
