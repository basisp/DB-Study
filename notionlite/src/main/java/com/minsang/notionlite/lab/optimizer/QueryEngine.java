package com.minsang.notionlite.lab.optimizer;

// 옵티마이저는 결국 테이블의 현재 상태를 보고 전략을 고릅니다.
import com.minsang.notionlite.lab.table.InMemoryTable;
// 결과로 반환할 행 타입입니다.
import com.minsang.notionlite.lab.table.Row;

// 조회 결과를 리스트로 모으기 위해 사용합니다.
import java.util.List;

/**
 * Tiny optimizer mimic.
 *
 * Rule:
 * - Small table: full scan
 * - Larger table: secondary index scan on title
 */
public class QueryEngine {
    // 행 수가 이 값보다 작으면 풀스캔이 더 단순하다고 가정합니다.
    private static final int INDEX_SCAN_THRESHOLD = 8;

    // 옵티마이저가 실제 데이터를 읽을 대상 테이블입니다.
    private final InMemoryTable table;

    public QueryEngine(InMemoryTable table) {
        this.table = table;
    }

    public QueryResult findByTitle(String title) {
        // 실행 계획은 보통 통계 정보나 테이블 크기를 보고 결정합니다.
        int tableSize = table.size();
        if (tableSize < INDEX_SCAN_THRESHOLD) {
            // 작은 테이블은 인덱스 타는 비용보다 순차 탐색이 더 단순할 수 있습니다.
            List<Row> rows = table.fullScanByTitle(title);
            return new QueryResult(
                    // explain 결과처럼 "왜 이 전략을 골랐는지"도 함께 보여줍니다.
                    new QueryPlan("full_scan", "small table favors sequential scan", tableSize),
                    rows
            );
        }

        // 테이블이 커지면 title 보조 인덱스를 타는 쪽이 유리하다고 가정합니다.
        List<Row> rows = table.selectByTitleSecondaryIndex(title);
        return new QueryResult(
                new QueryPlan("index_scan(title_bplus)", "secondary index expected to reduce scans", Math.max(1, rows.size())),
                rows
        );
    }
}
