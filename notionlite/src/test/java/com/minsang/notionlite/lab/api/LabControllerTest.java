package com.minsang.notionlite.lab.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rowInsertAndIndexDumpEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/lab/rows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "mysql",
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("mysql"));

        mockMvc.perform(get("/api/lab/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryBPlus").exists())
                .andExpect(jsonPath("$.primaryBTree").exists())
                .andExpect(jsonPath("$.secondaryTitleBPlus").exists());
    }
}
