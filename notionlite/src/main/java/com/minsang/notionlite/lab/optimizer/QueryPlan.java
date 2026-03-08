package com.minsang.notionlite.lab.optimizer;

/**
 * Simple explain-style output.
 */
public record QueryPlan(String strategy, String reason, int estimatedScannedRows) {
}
