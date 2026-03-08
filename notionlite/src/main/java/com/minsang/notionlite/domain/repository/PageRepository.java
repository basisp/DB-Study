package com.minsang.notionlite.domain.repository;

import com.minsang.notionlite.domain.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PageRepository extends JpaRepository<Page, Long> {
    List<Page> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);

    List<Page> findByWorkspaceIdAndTitleContainingIgnoreCaseOrderByCreatedAtAsc(Long workspaceId, String query);

    List<Page> findByParentId(Long parentId);
}
