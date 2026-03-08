package com.minsang.notionlite.lab.table;

import com.minsang.notionlite.lab.index.BPlusTree;
import com.minsang.notionlite.lab.index.BTree;

import java.util.*;

/**
 * Learning table with both clustered and secondary indexes.
 *
 * - heap: row storage (simulates table data)
 * - pkBPlusIndex: clustered index feel (id -> id)
 * - titleBPlusIndex: secondary index feel (title -> [id,id,...])
 * - pkBTree: separate classic B-Tree for side-by-side learning
 */
public class InMemoryTable {
    private Map<Long, Row> heap = new LinkedHashMap<>();
    private BPlusTree<Long, Long> pkBPlusIndex = new BPlusTree<>(4);
    private BPlusTree<String, List<Long>> titleBPlusIndex = new BPlusTree<>(4);
    private BTree<Long, Long> pkBTree = new BTree<>(2);
    private long nextId = 1L;

    public synchronized Row insert(String title, String content) {
        long id = nextId++;
        Row row = new Row(id, title, content);
        putRow(row);
        return row;
    }

    public synchronized void resetAndLoad(List<Row> rows) {
        heap = new LinkedHashMap<>();
        pkBPlusIndex = new BPlusTree<>(4);
        titleBPlusIndex = new BPlusTree<>(4);
        pkBTree = new BTree<>(2);
        nextId = 1L;

        for (Row row : rows) {
            putRow(row);
            nextId = Math.max(nextId, row.id() + 1);
        }
    }

    private void putRow(Row row) {
        heap.put(row.id(), row);
        pkBPlusIndex.insert(row.id(), row.id());
        pkBTree.insert(row.id(), row.id());

        List<Long> ids = titleBPlusIndex.search(row.title()).orElseGet(ArrayList::new);
        ids.add(row.id());
        titleBPlusIndex.insert(row.title(), ids);
    }

    public synchronized Optional<Row> selectById(long id) {
        return pkBPlusIndex.search(id).map(heap::get);
    }

    public synchronized List<Row> selectByTitleSecondaryIndex(String title) {
        List<Long> ids = titleBPlusIndex.search(title).orElse(List.of());
        List<Row> rows = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Row row = heap.get(id);
            if (row != null) {
                rows.add(row);
            }
        }
        return rows;
    }

    public synchronized List<Row> fullScanByTitle(String title) {
        List<Row> rows = new ArrayList<>();
        for (Row row : heap.values()) {
            if (row.title().equals(title)) {
                rows.add(row);
            }
        }
        return rows;
    }

    public synchronized List<Row> rangeByPrimaryKey(long fromInclusive, long toInclusive) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : pkBPlusIndex.rangeSearch(fromInclusive, toInclusive)) {
            Row row = heap.get(entry.getValue());
            if (row != null) {
                rows.add(row);
            }
        }
        return rows;
    }

    public synchronized int size() {
        return heap.size();
    }

    public synchronized List<Row> allRows() {
        return List.copyOf(heap.values());
    }

    public synchronized String dumpPrimaryBPlusStructure() {
        return pkBPlusIndex.debugStructure();
    }

    public synchronized String dumpPrimaryBTreeStructure() {
        return pkBTree.debugStructure();
    }

    public synchronized String dumpSecondaryTitleStructure() {
        return titleBPlusIndex.debugStructure();
    }
}
