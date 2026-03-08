package com.minsang.notionlite.lab.persistence;

import com.minsang.notionlite.lab.storage.RecordSerializer;
import com.minsang.notionlite.lab.storage.SlottedPage;
import com.minsang.notionlite.lab.table.Row;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes table rows into slotted pages, then to disk.
 */
public class TableSnapshotStore {
    private final int pageSize;
    private final PageFileStore fileStore;

    public TableSnapshotStore(int pageSize) {
        this.pageSize = pageSize;
        this.fileStore = new PageFileStore();
    }

    public void save(Path path, List<Row> rows) {
        List<SlottedPage> pages = new ArrayList<>();
        SlottedPage current = new SlottedPage(0, pageSize);

        for (Row row : rows) {
            byte[] payload = RecordSerializer.serialize(row);
            if (current.insert(payload).isEmpty()) {
                pages.add(current);
                current = new SlottedPage(pages.size(), pageSize);
                current.insert(payload).orElseThrow();
            }
        }

        if (!current.slots().isEmpty()) {
            pages.add(current);
        }

        fileStore.save(path, pages);
    }

    public List<Row> load(Path path) {
        List<SlottedPage> pages = fileStore.load(path);
        List<Row> rows = new ArrayList<>();

        for (SlottedPage page : pages) {
            for (int slotId = 0; slotId < page.slots().size(); slotId++) {
                page.read(slotId)
                        .map(RecordSerializer::deserialize)
                        .ifPresent(rows::add);
            }
        }

        return rows;
    }
}
