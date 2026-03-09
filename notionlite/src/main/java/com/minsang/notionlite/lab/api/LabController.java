package com.minsang.notionlite.lab.api;

// 쿼리 결과를 HTTP 응답으로 내보내기 위해 사용합니다.
import com.minsang.notionlite.lab.optimizer.QueryResult;
// 실제 DB 학습 로직은 서비스 계층에 모아두고 컨트롤러는 요청/응답만 담당합니다.
import com.minsang.notionlite.lab.service.LabDatabaseService;
// 테이블의 한 행(row)을 표현하는 데이터 구조입니다.
import com.minsang.notionlite.lab.table.Row;
// 201 Created 같은 HTTP 상태 코드를 쓰기 위해 가져옵니다.
import org.springframework.http.HttpStatus;
// REST API 매핑용 애너테이션들입니다.
import org.springframework.web.bind.annotation.*;
// 찾는 데이터가 없을 때 404를 던지기 위해 사용합니다.
import org.springframework.web.server.ResponseStatusException;

// 목록형 응답을 위해 사용합니다.
import java.util.List;
// 간단한 key-value JSON 응답을 만들기 위해 사용합니다.
import java.util.Map;

/**
 * REST endpoints = minimal UI for direct DB learning with curl/Postman.
 */
@RestController
@RequestMapping("/api/lab")
public class LabController {
    // 컨트롤러는 직접 저장소를 다루지 않고 서비스에게 일을 위임합니다.
    private final LabDatabaseService service;

    // 스프링이 서비스 객체를 주입해 줍니다.
    public LabController(LabDatabaseService service) {
        this.service = service;
    }

    // 새 row를 추가하는 POST API입니다.
    @PostMapping("/rows")
    // 생성 성공 시 201 상태를 반환합니다.
    @ResponseStatus(HttpStatus.CREATED)
    public Row insert(@RequestBody InsertRequest request) {
        // 요청 JSON에서 title/content를 꺼내 서비스에 전달합니다.
        return service.insert(request.title(), request.content());
    }

    // PK(id)로 한 건을 조회하는 GET API입니다.
    @GetMapping("/rows/{id}")
    public Row findById(@PathVariable long id) {
        // Optional이 비어 있으면 404 에러로 바꿔서 클라이언트에 알려줍니다.
        return service.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "row not found"));
    }

    // 현재 메모리에 있는 모든 행을 조회합니다.
    @GetMapping("/rows")
    public List<Row> allRows() {
        return service.allRows();
    }

    // title 조건으로 조회하고, 어떤 실행 계획을 탔는지도 함께 반환합니다.
    @GetMapping("/query/title")
    public QueryResult queryByTitle(@RequestParam String value) {
        return service.queryByTitle(value);
    }

    // 기본키 범위 검색을 실습하는 API입니다.
    @GetMapping("/query/pk-range")
    public List<Row> rangeById(@RequestParam long from, @RequestParam long to) {
        return service.rangeById(from, to);
    }

    // 내부 인덱스 구조를 문자열로 덤프해서 학습에 활용합니다.
    @GetMapping("/indexes")
    public Map<String, String> dumpIndexes() {
        return Map.of(
                // 기본키 B+Tree 구조입니다.
                "primaryBPlus", service.dumpPrimaryBPlus(),
                // 비교용 기본키 B-Tree 구조입니다.
                "primaryBTree", service.dumpPrimaryBTree(),
                // 보조 인덱스(title -> id 목록) 구조입니다.
                "secondaryTitleBPlus", service.dumpSecondaryTitle()
        );
    }

    // 현재 테이블 상태를 파일로 저장합니다.
    @PostMapping("/snapshot/save")
    public Map<String, String> saveSnapshot(@RequestBody SnapshotRequest request) {
        service.saveSnapshot(request.path());
        return Map.of("status", "saved", "path", request.path());
    }

    // 파일에서 테이블 상태를 다시 읽어옵니다.
    @PostMapping("/snapshot/load")
    public Map<String, Object> loadSnapshot(@RequestBody SnapshotRequest request) {
        List<Row> rows = service.loadSnapshot(request.path());
        return Map.of("status", "loaded", "path", request.path(), "rowCount", rows.size());
    }

    // insert 요청 본문을 담는 간단한 DTO입니다.
    public record InsertRequest(String title, String content) {
    }

    // snapshot 파일 경로를 받는 간단한 DTO입니다.
    public record SnapshotRequest(String path) {
    }
}
