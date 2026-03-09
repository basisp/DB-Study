package com.minsang.notionlite.lab.persistence;

// 페이지 단위 직렬화/역직렬화를 담당하는 자료구조입니다.
import com.minsang.notionlite.lab.storage.SlottedPage;

// 바이너리 파일 입출력용 클래스들입니다.
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists page list into one binary file.
 */
public class PageFileStore {
    // 파일 형식이 맞는지 검사하기 위한 시그니처 값입니다.
    private static final int MAGIC = 0xDB5A1234;

    public void save(Path path, List<SlottedPage> pages) {
        try {
            // 상위 폴더가 없으면 먼저 만들어야 파일 저장이 가능합니다.
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
                // 파일 맨 앞에 매직 넘버를 써서 우리 형식 파일인지 확인합니다.
                out.writeInt(MAGIC);
                // 몇 개의 페이지가 저장되는지 기록합니다.
                out.writeInt(pages.size());
                for (SlottedPage page : pages) {
                    // 각 페이지를 먼저 바이트 배열로 바꿉니다.
                    byte[] payload = page.toBytes();
                    // 길이를 먼저 저장해야 읽을 때 경계를 알 수 있습니다.
                    out.writeInt(payload.length);
                    // 실제 페이지 바이트를 저장합니다.
                    out.write(payload);
                }
            }
        } catch (IOException e) {
            // 체크 예외를 런타임 예외로 바꿔 호출부를 단순하게 유지합니다.
            throw new UncheckedIOException(e);
        }
    }

    public List<SlottedPage> load(Path path) {
        // 파일이 없으면 빈 스냅샷으로 취급합니다.
        if (!Files.exists(path)) {
            return List.of();
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            // 저장 시 썼던 매직 넘버를 다시 읽습니다.
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalStateException("Invalid snapshot file magic");
            }

            // 몇 개의 페이지를 읽어야 하는지 확인합니다.
            int pageCount = in.readInt();
            List<SlottedPage> pages = new ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                // 각 페이지의 바이트 길이와 실제 내용을 순서대로 읽습니다.
                int length = in.readInt();
                byte[] payload = in.readNBytes(length);
                // 바이트를 다시 SlottedPage 객체로 복원합니다.
                pages.add(SlottedPage.fromBytes(payload));
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
