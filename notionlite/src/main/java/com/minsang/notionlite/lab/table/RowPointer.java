package com.minsang.notionlite.lab.table;

/**
 * Logical address of a row inside heap pages.
 */
public record RowPointer(int pageId, int slotId) {
}
