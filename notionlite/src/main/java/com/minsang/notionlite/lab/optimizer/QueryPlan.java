package com.minsang.notionlite.lab.optimizer;

/**
 * Simple explain-style output.
 */
// strategy: 어떤 접근 방법을 골랐는지
// reason: 왜 그렇게 판단했는지
// estimatedScannedRows: 몇 행 정도 읽을 것으로 예상했는지
public record QueryPlan(String strategy, String reason, int estimatedScannedRows) {
}
