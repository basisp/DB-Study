package com.minsang.notionlite.lab.storage;

/**
 * A slot points to one record payload in a slotted page.
 *
 * offset/length locate bytes inside page body.
 * deleted marks tombstone-like removal for this basic model.
 */
public record Slot(int offset, int length, boolean deleted) {
    public Slot markDeleted() {
        return new Slot(offset, length, true);
    }
}
