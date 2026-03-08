package com.minsang.notionlite.lab.api;

import com.minsang.notionlite.lab.optimizer.QueryResult;
import com.minsang.notionlite.lab.service.LabDatabaseService;
import com.minsang.notionlite.lab.table.Row;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints = minimal UI for direct DB learning with curl/Postman.
 */
@RestController
@RequestMapping("/api/lab")
public class LabController {
    private final LabDatabaseService service;

    public LabController(LabDatabaseService service) {
        this.service = service;
    }

    @PostMapping("/rows")
    @ResponseStatus(HttpStatus.CREATED)
    public Row insert(@RequestBody InsertRequest request) {
        return service.insert(request.title(), request.content());
    }

    @GetMapping("/rows/{id}")
    public Row findById(@PathVariable long id) {
        return service.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "row not found"));
    }

    @GetMapping("/rows")
    public List<Row> allRows() {
        return service.allRows();
    }

    @GetMapping("/query/title")
    public QueryResult queryByTitle(@RequestParam String value) {
        return service.queryByTitle(value);
    }

    @GetMapping("/query/pk-range")
    public List<Row> rangeById(@RequestParam long from, @RequestParam long to) {
        return service.rangeById(from, to);
    }

    @GetMapping("/indexes")
    public Map<String, String> dumpIndexes() {
        return Map.of(
                "primaryBPlus", service.dumpPrimaryBPlus(),
                "primaryBTree", service.dumpPrimaryBTree(),
                "secondaryTitleBPlus", service.dumpSecondaryTitle()
        );
    }

    @PostMapping("/snapshot/save")
    public Map<String, String> saveSnapshot(@RequestBody SnapshotRequest request) {
        service.saveSnapshot(request.path());
        return Map.of("status", "saved", "path", request.path());
    }

    @PostMapping("/snapshot/load")
    public Map<String, Object> loadSnapshot(@RequestBody SnapshotRequest request) {
        List<Row> rows = service.loadSnapshot(request.path());
        return Map.of("status", "loaded", "path", request.path(), "rowCount", rows.size());
    }

    public record InsertRequest(String title, String content) {
    }

    public record SnapshotRequest(String path) {
    }
}
