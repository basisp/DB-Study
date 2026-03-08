package com.minsang.notionlite.domain.repository;

import com.minsang.notionlite.domain.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
