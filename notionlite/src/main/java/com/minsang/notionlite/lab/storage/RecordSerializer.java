package com.minsang.notionlite.lab.storage;

import com.minsang.notionlite.lab.table.Row;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Converts rows to bytes and back.
 *
 * This lets us simulate how a storage engine writes records into pages.
 */
public final class RecordSerializer {
    private RecordSerializer() {
    }

    public static byte[] serialize(Row row) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            // Fixed-length field first for easy parsing.
            out.writeLong(row.id());

            // Variable-length fields are stored as [length][bytes].
            writeString(out, row.title());
            writeString(out, row.content());

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Row deserialize(byte[] payload) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            long id = in.readLong();
            String title = readString(in);
            String content = readString(in);
            return new Row(id, title, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] bytes = in.readNBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
