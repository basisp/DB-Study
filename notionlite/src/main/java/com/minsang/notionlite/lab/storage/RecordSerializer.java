package com.minsang.notionlite.lab.storage;

// 직렬화 대상인 Row 타입입니다.
import com.minsang.notionlite.lab.table.Row;

// 바이트 스트림 기반 변환을 위해 사용합니다.
import java.io.*;
// 문자열을 바이트로 바꿀 때 문자 인코딩을 명시합니다.
import java.nio.charset.StandardCharsets;

/**
 * Converts rows to bytes and back.
 *
 * This lets us simulate how a storage engine writes records into pages.
 */
public final class RecordSerializer {
    // 유틸리티 클래스이므로 인스턴스 생성을 막습니다.
    private RecordSerializer() {
    }

    public static byte[] serialize(Row row) {
        try {
            // 메모리 상의 바이트 버퍼를 만듭니다.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // primitive 타입을 쉽게 쓰기 위한 래퍼입니다.
            DataOutputStream out = new DataOutputStream(baos);

            // Fixed-length field first for easy parsing.
            out.writeLong(row.id());

            // Variable-length fields are stored as [length][bytes].
            writeString(out, row.title());
            writeString(out, row.content());

            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            // ByteArrayOutputStream에서는 거의 안 나지만 인터페이스상 처리합니다.
            throw new UncheckedIOException(e);
        }
    }

    public static Row deserialize(byte[] payload) {
        try {
            // 바이트 배열을 다시 읽기 스트림으로 감쌉니다.
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
            // serialize와 같은 순서로 읽어야 원래 row를 복원할 수 있습니다.
            long id = in.readLong();
            String title = readString(in);
            String content = readString(in);
            return new Row(id, title, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        // 문자열을 UTF-8 바이트로 바꿉니다.
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        // 먼저 길이를 저장해야 경계가 없는 가변 길이 문자열을 정확히 읽을 수 있습니다.
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        // 저장할 때 먼저 썼던 길이를 읽습니다.
        int length = in.readInt();
        byte[] bytes = in.readNBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
