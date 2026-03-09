package com.minsang.notionlite.lab.table;

import com.minsang.notionlite.lab.index.BPlusTree;
import com.minsang.notionlite.lab.index.BTree;
import com.minsang.notionlite.lab.persistence.BufferPool;
import com.minsang.notionlite.lab.persistence.PageFileStore;
import com.minsang.notionlite.lab.storage.RecordSerializer;
import com.minsang.notionlite.lab.storage.SlottedPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;

/**
 * Learning table that mimics a page-based heap file:
 * - table rows are stored in slotted pages
 * - primary/secondary indexes keep row pointers, not row bodies
 * - buffer pool simulates page caching
 */
public class InMemoryTable {
    private static final int DEFAULT_PAGE_SIZE = 4096;
    private static final int DEFAULT_BUFFER_POOL_CAPACITY = 64;

    private final int pageSize;
    private final BufferPool<Integer, SlottedPage> bufferPool;
    private final PageFileStore pageFileStore = new PageFileStore();
    private final Path heapFilePath;

    private final List<SlottedPage> heapPages = new ArrayList<>();
    private BPlusTree<Long, RowPointer> pkBPlusIndex = new BPlusTree<>(4);
    private BPlusTree<String, List<RowPointer>> titleBPlusIndex = new BPlusTree<>(4);
    private BTree<Long, RowPointer> pkBTree = new BTree<>(2);

    private long nextId = 1L;
    private int rowCount = 0;

    public InMemoryTable() {
        this(DEFAULT_PAGE_SIZE, DEFAULT_BUFFER_POOL_CAPACITY, null);
    }

    public InMemoryTable(int pageSize, int bufferPoolCapacity) {
        this(pageSize, bufferPoolCapacity, null);
    }

    public InMemoryTable(int pageSize, int bufferPoolCapacity, Path heapFilePath) {
        this.pageSize = pageSize;
        this.bufferPool = new BufferPool<>(bufferPoolCapacity);
        this.heapFilePath = heapFilePath;
        initializeFromDiskIfConfigured();
    }

    private void initializeFromDiskIfConfigured() {
        if (heapFilePath == null) {
            return;
        }
        heapPages.clear();
        heapPages.addAll(pageFileStore.load(heapFilePath));
        rebuildIndexesFromHeapPages();
    }

    private void rebuildIndexesFromHeapPages() {
        pkBPlusIndex = new BPlusTree<>(4);
        titleBPlusIndex = new BPlusTree<>(4);
        pkBTree = new BTree<>(2);
        nextId = 1L;
        rowCount = 0;

        for (SlottedPage page : heapPages) {
            for (int slotId = 0; slotId < page.slots().size(); slotId++) {
                Optional<Row> maybeRow = page.read(slotId).map(RecordSerializer::deserialize);
                if (maybeRow.isEmpty()) {
                    continue;
                }
                Row row = maybeRow.get();
                RowPointer pointer = new RowPointer(page.pageId(), slotId);
                pkBPlusIndex.insert(row.id(), pointer);
                pkBTree.insert(row.id(), pointer);
                List<RowPointer> pointers = titleBPlusIndex.search(row.title()).orElseGet(ArrayList::new);
                pointers.add(pointer);
                titleBPlusIndex.insert(row.title(), pointers);
                nextId = Math.max(nextId, row.id() + 1);
                rowCount++;
            }
        }
    }

    public synchronized Row insert(String title, String content) {
        long id = nextId++;
        Row row = new Row(id, title, content);
        putRow(row);
        persistPagesIfConfigured();
        return row;
    }

    public synchronized void resetAndLoad(List<Row> rows) {
        heapPages.clear();
        pkBPlusIndex = new BPlusTree<>(4);
        titleBPlusIndex = new BPlusTree<>(4);
        pkBTree = new BTree<>(2);
        nextId = 1L;
        rowCount = 0;

        for (Row row : rows) {
            putRow(row);
            nextId = Math.max(nextId, row.id() + 1);
        }
        persistPagesIfConfigured();
    }

    private void putRow(Row row) {
        RowPointer pointer = appendRowToHeap(row);
        pkBPlusIndex.insert(row.id(), pointer);
        pkBTree.insert(row.id(), pointer);

        List<RowPointer> pointers = titleBPlusIndex.search(row.title()).orElseGet(ArrayList::new);
        pointers.add(pointer);
        titleBPlusIndex.insert(row.title(), pointers);
        rowCount++;
    }

    private RowPointer appendRowToHeap(Row row) {
        byte[] payload = RecordSerializer.serialize(row);
        SlottedPage page = writableTailPage();
        Optional<Integer> slotId = page.insert(payload);
        if (slotId.isPresent()) {
            return new RowPointer(page.pageId(), slotId.get());
        }

        SlottedPage newPage = createNewPage();
        int newSlot = newPage.insert(payload).orElseThrow();
        return new RowPointer(newPage.pageId(), newSlot);
    }

    private SlottedPage writableTailPage() {
        if (heapPages.isEmpty()) {
            return createNewPage();
        }
        int pageId = heapPages.size() - 1;
        return bufferPool.getOrLoad(pageId, this::loadPage);
    }

    private SlottedPage createNewPage() {
        int pageId = heapPages.size();
        SlottedPage page = new SlottedPage(pageId, pageSize);
        heapPages.add(page);
        return bufferPool.getOrLoad(pageId, ignored -> page);
    }

    private SlottedPage loadPage(Integer pageId) {
        if (pageId < 0 || pageId >= heapPages.size()) {
            throw new IllegalArgumentException("unknown pageId: " + pageId);
        }
        return heapPages.get(pageId);
    }

    private void persistPagesIfConfigured() {
        if (heapFilePath == null) {
            return;
        }
        pageFileStore.save(heapFilePath, heapPages);
    }

    private Optional<Row> readPointer(RowPointer pointer) {
        SlottedPage page = bufferPool.getOrLoad(pointer.pageId(), this::loadPage);
        return page.read(pointer.slotId()).map(RecordSerializer::deserialize);
    }

    public synchronized Optional<Row> selectById(long id) {
        return pkBPlusIndex.search(id).flatMap(this::readPointer);
    }

    public synchronized List<Row> selectByTitleSecondaryIndex(String title) {
        List<RowPointer> pointers = titleBPlusIndex.search(title).orElse(List.of());
        List<Row> rows = new ArrayList<>(pointers.size());
        for (RowPointer pointer : pointers) {
            readPointer(pointer).ifPresent(rows::add);
        }
        return rows;
    }

    public synchronized List<Row> fullScanByTitle(String title) {
        List<Row> rows = new ArrayList<>();
        for (SlottedPage page : heapPages) {
            SlottedPage loaded = bufferPool.getOrLoad(page.pageId(), this::loadPage);
            for (int slotId = 0; slotId < loaded.slots().size(); slotId++) {
                loaded.read(slotId)
                        .map(RecordSerializer::deserialize)
                        .filter(row -> row.title().equals(title))
                        .ifPresent(rows::add);
            }
        }
        return rows;
    }

    public synchronized List<Row> rangeByPrimaryKey(long fromInclusive, long toInclusive) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, RowPointer> entry : pkBPlusIndex.rangeSearch(fromInclusive, toInclusive)) {
            readPointer(entry.getValue()).ifPresent(rows::add);
        }
        return rows;
    }

    public synchronized int size() {
        return rowCount;
    }

    public synchronized List<Row> allRows() {
        List<Row> rows = new ArrayList<>(rowCount);
        for (SlottedPage page : heapPages) {
            SlottedPage loaded = bufferPool.getOrLoad(page.pageId(), this::loadPage);
            for (int slotId = 0; slotId < loaded.slots().size(); slotId++) {
                loaded.read(slotId)
                        .map(RecordSerializer::deserialize)
                        .ifPresent(rows::add);
            }
        }
        return rows;
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
