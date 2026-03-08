package com.minsang.notionlite.lab.index;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BPlusTreeTest {

    @Test
    void supportsSearchInsertSplitAndRange() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);

        // Enough inserts to force multiple splits.
        for (int i = 1; i <= 20; i++) {
            tree.insert(i, "v" + i);
        }

        assertEquals("v7", tree.search(7).orElseThrow());
        assertTrue(tree.search(100).isEmpty());

        List<Map.Entry<Integer, String>> range = tree.rangeSearch(5, 8);
        assertEquals(List.of(5, 6, 7, 8), range.stream().map(Map.Entry::getKey).toList());
    }
}
