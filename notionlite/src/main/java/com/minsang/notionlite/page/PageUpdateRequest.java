package com.minsang.notionlite.page;

import com.fasterxml.jackson.databind.JsonNode;

public class PageUpdateRequest {
    private String title;
    private JsonNode parentId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getParentId() {
        return parentId;
    }

    public void setParentId(JsonNode parentId) {
        this.parentId = parentId;
    }

    public boolean hasTitle() {
        return title != null;
    }

    public boolean hasParentId() {
        return parentId != null;
    }

    public Long parseParentId() {
        if (parentId == null || parentId.isNull()) {
            return null;
        }
        if (!parentId.isIntegralNumber()) {
            throw new IllegalArgumentException("parentId must be a number or null");
        }
        return parentId.asLong();
    }
}
