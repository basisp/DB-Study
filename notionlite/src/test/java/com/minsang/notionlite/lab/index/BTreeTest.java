package com.minsang.notionlite.lab.index;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies baseline B-Tree behavior for learning:
 * - insert
 * - point search
 * - range scan
 */
class BTreeTest {

    @Test
    void supportsSearchAndRange() {
        // Use minimum degree 2 so split behavior appears quickly.
        BTree<Integer, String> tree = new BTree<>(2);
        for (int i = 1; i <= 20; i++) {
            tree.insert(i, "v" + i);
        }

        // Point lookup returns inserted value.
        assertEquals("v12", tree.search(12).orElseThrow());
        // Unknown key should be absent.
        assertTrue(tree.search(99).isEmpty());

        // Range lookup should return sorted key/value pairs.
        List<Map.Entry<Integer, String>> range = tree.rangeSearch(10, 13);
        assertEquals(List.of(10, 11, 12, 13), range.stream().map(Map.Entry::getKey).toList());
    }
}
