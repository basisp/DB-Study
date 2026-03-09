package com.minsang.notionlite.lab.persistence;

// Row를 바이트로 바꾸고 다시 복원하는 유틸리티입니다.
import com.minsang.notionlite.lab.storage.RecordSerializer;
// 실제 저장 단위인 slotted page 구조입니다.
import com.minsang.notionlite.lab.storage.SlottedPage;
// 논리적 테이블의 한 행입니다.
import com.minsang.notionlite.lab.table.Row;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes table rows into slotted pages, then to disk.
 */
public class TableSnapshotStore {
    // 페이지 하나의 크기입니다. 실제 DB에서도 page size는 핵심 설정입니다.
    private final int pageSize;
    // 여러 페이지를 파일 한 개로 저장/로드하는 하위 저장소입니다.
    private final PageFileStore fileStore;

    public TableSnapshotStore(int pageSize) {
        this.pageSize = pageSize;
        this.fileStore = new PageFileStore();
    }

    public void save(Path path, List<Row> rows) {
        // 행들을 여러 페이지로 나눠 담기 위한 리스트입니다.
        List<SlottedPage> pages = new ArrayList<>();
        // 우선 첫 페이지 하나를 준비합니다.
        SlottedPage current = new SlottedPage(0, pageSize);

        for (Row row : rows) {
            // 행 한 건을 디스크에 쓰기 쉬운 바이트 배열로 변환합니다.
            byte[] payload = RecordSerializer.serialize(row);
            if (current.insert(payload).isEmpty()) {
                // 현재 페이지에 공간이 부족하면 페이지를 닫고 새 페이지를 엽니다.
                pages.add(current);
                current = new SlottedPage(pages.size(), pageSize);
                // 새 페이지에는 반드시 들어가야 합니다.
                current.insert(payload).orElseThrow();
            }
        }

        // 마지막 작업 중인 페이지에 데이터가 있으면 저장 목록에 포함합니다.
        if (!current.slots().isEmpty()) {
            pages.add(current);
        }

        // 페이지 리스트를 최종적으로 파일에 기록합니다.
        fileStore.save(path, pages);
    }

    public List<Row> load(Path path) {
        // 파일에서 페이지들을 읽어옵니다.
        List<SlottedPage> pages = fileStore.load(path);
        List<Row> rows = new ArrayList<>();

        for (SlottedPage page : pages) {
            // 슬롯 디렉터리를 처음부터 끝까지 순회합니다.
            for (int slotId = 0; slotId < page.slots().size(); slotId++) {
                // 삭제되지 않은 슬롯만 읽어서 Row 객체로 되살립니다.
                page.read(slotId)
                        .map(RecordSerializer::deserialize)
                        .ifPresent(rows::add);
            }
        }

        return rows;
    }
}
