package com.minsang.notionlite.workspace;

import com.minsang.notionlite.common.ResourceNotFoundException;
import com.minsang.notionlite.domain.entity.Workspace;
import com.minsang.notionlite.domain.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public List<WorkspaceResponse> getWorkspaces() {
        return workspaceRepository.findAll().stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    public WorkspaceResponse getWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        return WorkspaceResponse.from(workspace);
    }

    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        Workspace saved = workspaceRepository.save(workspace);
        return WorkspaceResponse.from(saved);
    }
}
