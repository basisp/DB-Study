package com.minsang.notionlite.lab.optimizer;

import com.minsang.notionlite.lab.table.InMemoryTable;
import com.minsang.notionlite.lab.table.Row;

import java.util.List;

/**
 * Tiny optimizer mimic.
 *
 * Rule:
 * - Small table: full scan
 * - Larger table: secondary index scan on title
 */
public class QueryEngine {
    private static final int INDEX_SCAN_THRESHOLD = 8;

    private final InMemoryTable table;

    public QueryEngine(InMemoryTable table) {
        this.table = table;
    }

    public QueryResult findByTitle(String title) {
        int tableSize = table.size();
        if (tableSize < INDEX_SCAN_THRESHOLD) {
            List<Row> rows = table.fullScanByTitle(title);
            return new QueryResult(
                    new QueryPlan("full_scan", "small table favors sequential scan", tableSize),
                    rows
            );
        }

        List<Row> rows = table.selectByTitleSecondaryIndex(title);
        return new QueryResult(
                new QueryPlan("index_scan(title_bplus)", "secondary index expected to reduce scans", Math.max(1, rows.size())),
                rows
        );
    }
}
