package com.minsang.notionlite.page;

import com.minsang.notionlite.common.BadRequestException;
import com.minsang.notionlite.common.ResourceNotFoundException;
import com.minsang.notionlite.domain.entity.Page;
import com.minsang.notionlite.domain.entity.Workspace;
import com.minsang.notionlite.domain.repository.PageRepository;
import com.minsang.notionlite.domain.repository.WorkspaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class PageService {
    private final PageRepository pageRepository;
    private final WorkspaceRepository workspaceRepository;

    public PageService(PageRepository pageRepository, WorkspaceRepository workspaceRepository) {
        this.pageRepository = pageRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public List<PageResponse> getPages(Long workspaceId) {
        List<Page> pages = workspaceId == null
                ? pageRepository.findAll()
                : pageRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
        return pages.stream().map(PageResponse::from).toList();
    }

    public List<PageResponse> searchPages(Long workspaceId, String query) {
        if (workspaceId == null) {
            throw new BadRequestException("workspaceId is required");
        }
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return getPages(workspaceId);
        }
        return pageRepository.findByWorkspaceIdAndTitleContainingIgnoreCaseOrderByCreatedAtAsc(workspaceId, normalized)
                .stream()
                .map(PageResponse::from)
                .toList();
    }

    public PageResponse getPage(Long pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        return PageResponse.from(page);
    }

    public PageResponse createPage(PageCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        Page parent = null;
        if (request.parentId() != null) {
            parent = pageRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent page not found"));
            if (!parent.getWorkspace().getId().equals(workspace.getId())) {
                throw new BadRequestException("Parent page belongs to another workspace");
            }
        }

        Page page = new Page();
        page.setWorkspace(workspace);
        page.setTitle(request.title().trim());
        page.setParent(parent);

        return PageResponse.from(pageRepository.save(page));
    }

    public PageResponse updatePage(Long pageId, PageUpdateRequest request) {
        if (!request.hasTitle() && !request.hasParentId()) {
            throw new BadRequestException("At least one of title or parentId is required");
        }

        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        if (request.hasTitle()) {
            String title = request.getTitle() == null ? "" : request.getTitle().trim();
            if (title.isEmpty()) {
                throw new BadRequestException("title cannot be blank");
            }
            page.setTitle(title);
        }

        if (request.hasParentId()) {
            Long parentId = request.parseParentId();
            Page nextParent = null;

            if (parentId != null) {
                if (parentId.equals(page.getId())) {
                    throw new BadRequestException("A page cannot be its own parent");
                }

                nextParent = pageRepository.findById(parentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent page not found"));

                if (!nextParent.getWorkspace().getId().equals(page.getWorkspace().getId())) {
                    throw new BadRequestException("Parent page belongs to another workspace");
                }

                if (isDescendant(page, nextParent)) {
                    throw new BadRequestException("Cannot move page under its descendant");
                }
            }

            page.setParent(nextParent);
        }

        return PageResponse.from(pageRepository.save(page));
    }

    public void deletePage(Long pageId) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        deleteRecursively(page);
    }

    private void deleteRecursively(Page page) {
        List<Page> children = pageRepository.findByParentId(page.getId());
        for (Page child : children) {
            deleteRecursively(child);
        }
        pageRepository.delete(page);
    }

    private boolean isDescendant(Page source, Page candidateParent) {
        Page current = candidateParent;
        while (current != null) {
            if (current.getId().equals(source.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
