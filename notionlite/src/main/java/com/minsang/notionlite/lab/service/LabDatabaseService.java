package com.minsang.notionlite.lab.service;

import com.minsang.notionlite.lab.optimizer.QueryEngine;
import com.minsang.notionlite.lab.optimizer.QueryResult;
import com.minsang.notionlite.lab.persistence.TableSnapshotStore;
import com.minsang.notionlite.lab.table.InMemoryTable;
import com.minsang.notionlite.lab.table.Row;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Application service that exposes learning DB operations.
 */
@Service
public class LabDatabaseService {
    private final InMemoryTable table = new InMemoryTable();
    private final QueryEngine queryEngine = new QueryEngine(table);
    private final TableSnapshotStore snapshotStore = new TableSnapshotStore(4096);

    public synchronized Row insert(String title, String content) {
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
        snapshotStore.save(Path.of(filePath), table.allRows());
    }

    public synchronized List<Row> loadSnapshot(String filePath) {
        List<Row> rows = snapshotStore.load(Path.of(filePath));
        table.resetAndLoad(rows);
        return rows;
    }

    public synchronized String dumpPrimaryBPlus() {
        return table.dumpPrimaryBPlusStructure();
    }

    public synchronized String dumpPrimaryBTree() {
        return table.dumpPrimaryBTreeStructure();
    }

    public synchronized String dumpSecondaryTitle() {
        return table.dumpSecondaryTitleStructure();
    }
}
