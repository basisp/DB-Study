package com.minsang.notionlite.lab.persistence;

import com.minsang.notionlite.lab.table.Row;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableSnapshotStoreTest {

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        TableSnapshotStore store = new TableSnapshotStore(256);
        List<Row> rows = List.of(
                new Row(1, "a", "hello"),
                new Row(2, "b", "world")
        );

        Path file = Files.createTempFile("db-lab", ".bin");
        store.save(file, rows);

        List<Row> loaded = store.load(file);
        assertEquals(rows, loaded);
    }
}
