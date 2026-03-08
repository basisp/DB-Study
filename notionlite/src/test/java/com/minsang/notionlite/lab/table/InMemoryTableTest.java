package com.minsang.notionlite.lab.table;

import com.minsang.notionlite.lab.optimizer.QueryEngine;
import com.minsang.notionlite.lab.optimizer.QueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTableTest {

    @Test
    void optimizerSwitchesFromFullScanToIndexScan() {
        InMemoryTable table = new InMemoryTable();
        QueryEngine engine = new QueryEngine(table);

        // Small table => full scan.
        table.insert("mysql", "a");
        table.insert("postgres", "b");
        QueryResult smallPlan = engine.findByTitle("mysql");
        assertEquals("full_scan", smallPlan.plan().strategy());

        // Larger table => index scan.
        for (int i = 0; i < 10; i++) {
            table.insert("mysql", "row-" + i);
        }
        QueryResult largePlan = engine.findByTitle("mysql");
        assertEquals("index_scan(title_bplus)", largePlan.plan().strategy());
        assertFalse(largePlan.rows().isEmpty());
    }
}
