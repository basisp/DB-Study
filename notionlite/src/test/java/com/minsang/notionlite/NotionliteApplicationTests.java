package com.minsang.notionlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotionliteApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void workspaceCreateAndList() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map("name", "My Workspace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("My Workspace"));

        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void pageCreateAndCycleGuard() throws Exception {
        String root = mockMvc.perform(post("/api/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "workspaceId", 1,
                                "title", "Root",
                                "parentId", null
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long rootId = objectMapper.readTree(root).get("id").asLong();

        String child = mockMvc.perform(post("/api/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "workspaceId", 1,
                                "title", "Child",
                                "parentId", rootId
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long childId = objectMapper.readTree(child).get("id").asLong();

        mockMvc.perform(patch("/api/pages/{pageId}", rootId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map("parentId", childId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot move page under its descendant"));
    }

    @Test
    void blockLifecycleAndReorder() throws Exception {
        String page = mockMvc.perform(post("/api/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "workspaceId", 1,
                                "title", "Block Page",
                                "parentId", null
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long pageId = objectMapper.readTree(page).get("id").asLong();

        String blockA = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "type", "paragraph",
                                "content", "A"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String blockB = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "type", "paragraph",
                                "content", "B"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long blockAId = objectMapper.readTree(blockA).get("id").asLong();
        Long blockBId = objectMapper.readTree(blockB).get("id").asLong();

        mockMvc.perform(put("/api/pages/{pageId}/blocks/reorder", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map("blockIds", List.of(blockBId, blockAId)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pages/{pageId}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockBId))
                .andExpect(jsonPath("$[0].positionIndex").value(0));
    }

    @Test
    void deleteMiddleBlockShouldReindexWithoutOrderRestriction() throws Exception {
        String page = mockMvc.perform(post("/api/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "workspaceId", 1,
                                "title", "Delete Reindex Page",
                                "parentId", null
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long pageId = objectMapper.readTree(page).get("id").asLong();

        String blockA = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "type", "paragraph",
                                "content", "A"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String blockB = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "type", "paragraph",
                                "content", "B"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String blockC = mockMvc.perform(post("/api/pages/{pageId}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "type", "paragraph",
                                "content", "C"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long blockAId = objectMapper.readTree(blockA).get("id").asLong();
        Long blockBId = objectMapper.readTree(blockB).get("id").asLong();
        Long blockCId = objectMapper.readTree(blockC).get("id").asLong();

        mockMvc.perform(delete("/api/pages/{pageId}/blocks/{blockId}", pageId, blockBId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pages/{pageId}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockAId))
                .andExpect(jsonPath("$[0].positionIndex").value(0))
                .andExpect(jsonPath("$[1].id").value(blockCId))
                .andExpect(jsonPath("$[1].positionIndex").value(1));
    }

    @Test
    void validationErrorFormat() throws Exception {
        mockMvc.perform(post("/api/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(map(
                                "title", "no workspace"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void notFoundErrorFormat() throws Exception {
        mockMvc.perform(get("/api/pages/{pageId}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Page not found"));
    }

    private static Map<String, Object> map(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must be pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return result;
    }
}
