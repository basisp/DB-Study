package com.minsang.notionlite.page;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PageController {
    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/pages")
    public List<PageResponse> getPages(@RequestParam(required = false) Long workspaceId) {
        return pageService.getPages(workspaceId);
    }

    @GetMapping("/pages/{pageId}")
    public PageResponse getPage(@PathVariable Long pageId) {
        return pageService.getPage(pageId);
    }

    @PostMapping("/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public PageResponse createPage(@Valid @RequestBody PageCreateRequest request) {
        return pageService.createPage(request);
    }

    @PatchMapping("/pages/{pageId}")
    public PageResponse updatePage(@PathVariable Long pageId, @RequestBody PageUpdateRequest request) {
        return pageService.updatePage(pageId, request);
    }

    @DeleteMapping("/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePage(@PathVariable Long pageId) {
        pageService.deletePage(pageId);
    }

    @GetMapping("/search/pages")
    public List<PageResponse> searchPages(@RequestParam(required = false) Long workspaceId,
                                          @RequestParam(required = false) String query) {
        return pageService.searchPages(workspaceId, query);
    }
}
