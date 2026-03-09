package com.minsang.notionlite.lab.storage;

// 페이지 자체를 바이트로 저장/로드하기 위한 입출력 클래스입니다.
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal slotted-page implementation.
 *
 * - Record bytes are packed from the end of page downward.
 * - Slot directory grows from the beginning.
 * - Insert fails when body and slot directory collide.
 */
public class SlottedPage {
    // 페이지 번호입니다. 실제 DB에서도 page id는 디스크 위치 추적에 중요합니다.
    private final int pageId;
    // 페이지의 전체 바이트 크기입니다.
    private final int pageSize;
    // 실제 레코드 바이트가 저장되는 본문 영역입니다.
    private final byte[] data;
    // 슬롯 디렉터리입니다. 각 슬롯은 레코드 위치와 길이를 가리킵니다.
    private final List<Slot> slots;
    // 아직 사용하지 않은 본문 영역의 시작 위치입니다.
    private int freeStart;

    public SlottedPage(int pageId, int pageSize) {
        this.pageId = pageId;
        this.pageSize = pageSize;
        this.data = new byte[pageSize];
        this.slots = new ArrayList<>();
        this.freeStart = pageSize;
    }

    private SlottedPage(int pageId, int pageSize, byte[] data, List<Slot> slots, int freeStart) {
        this.pageId = pageId;
        this.pageSize = pageSize;
        this.data = data;
        this.slots = slots;
        this.freeStart = freeStart;
    }

    public int pageId() {
        return pageId;
    }

    public List<Slot> slots() {
        return List.copyOf(slots);
    }

    public Optional<Integer> insert(byte[] payload) {
        // 새 슬롯까지 포함했을 때 슬롯 디렉터리가 차지할 공간입니다.
        int slotDirectoryBytes = (slots.size() + 1) * slotBytes();
        // 레코드는 페이지 끝에서 앞으로 채워 넣습니다.
        int writeOffset = freeStart - payload.length;

        // 슬롯 디렉터리와 레코드 본문이 겹치면 더 이상 삽입할 수 없습니다.
        if (writeOffset < slotDirectoryBytes) {
            return Optional.empty();
        }

        // payload를 계산한 위치에 복사합니다.
        System.arraycopy(payload, 0, data, writeOffset, payload.length);
        // 슬롯 디렉터리에 "어디에 저장했는지" 메타데이터를 추가합니다.
        slots.add(new Slot(writeOffset, payload.length, false));
        // 다음 삽입을 위해 여유 공간 시작점을 앞으로 당깁니다.
        freeStart = writeOffset;
        // 방금 추가한 슬롯 번호를 반환합니다.
        return Optional.of(slots.size() - 1);
    }

    public Optional<byte[]> read(int slotId) {
        // 잘못된 슬롯 번호는 읽을 수 없습니다.
        if (slotId < 0 || slotId >= slots.size()) {
            return Optional.empty();
        }
        Slot slot = slots.get(slotId);
        // tombstone 처리된 레코드는 없는 것으로 간주합니다.
        if (slot.deleted()) {
            return Optional.empty();
        }

        // 슬롯 메타데이터를 이용해 원본 바이트를 복사해 옵니다.
        byte[] payload = new byte[slot.length()];
        System.arraycopy(data, slot.offset(), payload, 0, slot.length());
        return Optional.of(payload);
    }

    public boolean delete(int slotId) {
        // 삭제 가능한 슬롯 번호인지 먼저 검사합니다.
        if (slotId < 0 || slotId >= slots.size()) {
            return false;
        }
        Slot slot = slots.get(slotId);
        // 이미 삭제된 슬롯은 다시 삭제하지 않습니다.
        if (slot.deleted()) {
            return false;
        }

        // 실제 바이트는 남겨두고 슬롯 상태만 deleted로 바꿉니다.
        slots.set(slotId, slot.markDeleted());
        return true;
    }

    public byte[] toBytes() {
        try {
            // 페이지 자체를 파일에 저장하기 위해 다시 바이트로 직렬화합니다.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            // 메타데이터부터 써야 복원할 때 해석이 가능합니다.
            out.writeInt(pageId);
            out.writeInt(pageSize);
            out.writeInt(freeStart);
            out.writeInt(slots.size());

            for (Slot slot : slots) {
                // 슬롯 디렉터리 정보도 함께 저장해야 각 레코드 위치를 찾을 수 있습니다.
                out.writeInt(slot.offset());
                out.writeInt(slot.length());
                out.writeBoolean(slot.deleted());
            }

            // 마지막으로 본문 바이트 배열 전체를 씁니다.
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SlottedPage fromBytes(byte[] payload) {
        try {
            // 저장 포맷과 같은 순서로 메타데이터와 본문을 다시 읽습니다.
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            int pageId = in.readInt();
            int pageSize = in.readInt();
            int freeStart = in.readInt();
            int slotCount = in.readInt();

            List<Slot> slots = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                slots.add(new Slot(in.readInt(), in.readInt(), in.readBoolean()));
            }

            int dataLength = in.readInt();
            byte[] data = in.readNBytes(dataLength);
            return new SlottedPage(pageId, pageSize, data, slots, freeStart);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int slotBytes() {
        // offset 4바이트 + length 4바이트 + deleted 1바이트입니다.
        return Integer.BYTES + Integer.BYTES + 1;
    }
}
