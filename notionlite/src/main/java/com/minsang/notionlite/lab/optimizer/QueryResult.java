package com.minsang.notionlite.lab.optimizer;

// 실제 조회된 row 목록입니다.
import com.minsang.notionlite.lab.table.Row;

// 여러 결과 행을 담기 위해 사용합니다.
import java.util.List;

/**
 * Query rows + explain plan.
 */
// plan은 "어떻게 찾았는지", rows는 "무엇을 찾았는지"를 뜻합니다.
public record QueryResult(QueryPlan plan, List<Row> rows) {
}
