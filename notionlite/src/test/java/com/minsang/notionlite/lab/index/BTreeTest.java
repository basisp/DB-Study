package com.minsang.notionlite.lab.index;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BTreeTest {

    @Test
    void supportsSearchAndRange() {
        BTree<Integer, String> tree = new BTree<>(2);
        for (int i = 1; i <= 20; i++) {
            tree.insert(i, "v" + i);
        }

        assertEquals("v12", tree.search(12).orElseThrow());
        assertTrue(tree.search(99).isEmpty());

        List<Map.Entry<Integer, String>> range = tree.rangeSearch(10, 13);
        assertEquals(List.of(10, 11, 12, 13), range.stream().map(Map.Entry::getKey).toList());
    }
}
