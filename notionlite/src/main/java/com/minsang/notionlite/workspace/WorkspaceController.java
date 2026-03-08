package com.minsang.notionlite.workspace;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceResponse> getWorkspaces() {
        return workspaceService.getWorkspaces();
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getWorkspace(@PathVariable Long workspaceId) {
        return workspaceService.getWorkspace(workspaceId);
    }

    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody WorkspaceCreateRequest request) {
        return workspaceService.createWorkspace(request);
    }
}
