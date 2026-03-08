package com.minsang.notionlite.block;

public class BlockUpdateRequest {
    private String type;
    private String content;
    private Integer positionIndex;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPositionIndex() {
        return positionIndex;
    }

    public void setPositionIndex(Integer positionIndex) {
        this.positionIndex = positionIndex;
    }

    public boolean hasAnyField() {
        return type != null || content != null || positionIndex != null;
    }
}
