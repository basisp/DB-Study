package com.minsang.notionlite.lab.optimizer;

import com.minsang.notionlite.lab.table.Row;

import java.util.List;

/**
 * Query rows + explain plan.
 */
public record QueryResult(QueryPlan plan, List<Row> rows) {
}
