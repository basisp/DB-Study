package com.minsang.notionlite.lab.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlottedPageTest {

    @Test
    void insertReadAndDeleteFlowWorks() {
        // Given a fresh page.
        SlottedPage page = new SlottedPage(1, 256);

        // When inserting one payload.
        int slotId = page.insert("hello".getBytes()).orElseThrow();

        // Then we can read it back.
        assertEquals("hello", new String(page.read(slotId).orElseThrow()));

        // And after delete, read returns empty.
        assertTrue(page.delete(slotId));
        assertTrue(page.read(slotId).isEmpty());
    }
}
