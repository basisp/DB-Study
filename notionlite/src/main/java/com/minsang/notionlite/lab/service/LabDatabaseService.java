package com.minsang.notionlite.lab.service;

// 질의 실행 계획을 고르는 미니 옵티마이저입니다.
import com.minsang.notionlite.lab.optimizer.QueryEngine;
import com.minsang.notionlite.lab.optimizer.QueryResult;
// 메모리 테이블을 파일로 저장/복원합니다.
import com.minsang.notionlite.lab.persistence.TableSnapshotStore;
// 학습용 테이블 본체입니다.
import com.minsang.notionlite.lab.table.InMemoryTable;
import com.minsang.notionlite.lab.table.Row;
// 스프링 서비스 빈으로 등록합니다.
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Application service that exposes learning DB operations.
 */
@Service
public class LabDatabaseService {
    // 실제 row 저장과 인덱스 관리를 담당합니다.
    private final InMemoryTable table = new InMemoryTable(4096, 64, Path.of("data", "heap-pages.bin"));
    // 같은 테이블을 바탕으로 실행 계획을 선택합니다.
    private final QueryEngine queryEngine = new QueryEngine(table);
    // 스냅샷 저장 시 페이지 크기는 4KB로 둡니다.
    private final TableSnapshotStore snapshotStore = new TableSnapshotStore(4096);

    public synchronized Row insert(String title, String content) {
        // synchronized로 묶어 간단한 동시성 문제를 피합니다.
        return table.insert(title, content);
    }

    public synchronized Optional<Row> findById(long id) {
        return table.selectById(id);
    }

    public synchronized QueryResult queryByTitle(String title) {
        return queryEngine.findByTitle(title);
    }

    public synchronized List<Row> rangeById(long fromInclusive, long toInclusive) {
        return table.rangeByPrimaryKey(fromInclusive, toInclusive);
    }

    public synchronized List<Row> allRows() {
        return table.allRows();
    }

    public synchronized void saveSnapshot(String filePath) {
        // 현재 메모리 상태를 파일 경로 기준으로 저장합니다.
        snapshotStore.save(Path.of(filePath), table.allRows());
    }

    public synchronized List<Row> loadSnapshot(String filePath) {
        // 파일에서 읽은 뒤, 기존 메모리 테이블을 새 상태로 교체합니다.
        List<Row> rows = snapshotStore.load(Path.of(filePath));
        table.resetAndLoad(rows);
        return rows;
    }

    public synchronized String dumpPrimaryBPlus() {
        // 인덱스 내부 구조를 문자열로 보여주면 학습에 도움이 됩니다.
        return table.dumpPrimaryBPlusStructure();
    }

    public synchronized String dumpPrimaryBTree() {
        return table.dumpPrimaryBTreeStructure();
    }

    public synchronized String dumpSecondaryTitle() {
        return table.dumpSecondaryTitleStructure();
    }
}
