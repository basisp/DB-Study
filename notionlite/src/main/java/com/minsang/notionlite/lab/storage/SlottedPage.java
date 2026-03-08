package com.minsang.notionlite.lab.storage;

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
    private final int pageId;
    private final int pageSize;
    private final byte[] data;
    private final List<Slot> slots;
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
        int slotDirectoryBytes = (slots.size() + 1) * slotBytes();
        int writeOffset = freeStart - payload.length;

        if (writeOffset < slotDirectoryBytes) {
            return Optional.empty();
        }

        System.arraycopy(payload, 0, data, writeOffset, payload.length);
        slots.add(new Slot(writeOffset, payload.length, false));
        freeStart = writeOffset;
        return Optional.of(slots.size() - 1);
    }

    public Optional<byte[]> read(int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return Optional.empty();
        }
        Slot slot = slots.get(slotId);
        if (slot.deleted()) {
            return Optional.empty();
        }

        byte[] payload = new byte[slot.length()];
        System.arraycopy(data, slot.offset(), payload, 0, slot.length());
        return Optional.of(payload);
    }

    public boolean delete(int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return false;
        }
        Slot slot = slots.get(slotId);
        if (slot.deleted()) {
            return false;
        }

        slots.set(slotId, slot.markDeleted());
        return true;
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeInt(pageId);
            out.writeInt(pageSize);
            out.writeInt(freeStart);
            out.writeInt(slots.size());

            for (Slot slot : slots) {
                out.writeInt(slot.offset());
                out.writeInt(slot.length());
                out.writeBoolean(slot.deleted());
            }

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
        return Integer.BYTES + Integer.BYTES + 1;
    }
}
