package com.minsang.notionlite.lab.storage;

/**
 * A slot points to one record payload in a slotted page.
 *
 * offset/length locate bytes inside page body.
 * deleted marks tombstone-like removal for this basic model.
 */
public record Slot(int offset, int length, boolean deleted) {
    public Slot markDeleted() {
        // 레코드 바이트는 남겨두고 "삭제됨" 표시만 바꾸는 방식입니다.
        return new Slot(offset, length, true);
    }
}
