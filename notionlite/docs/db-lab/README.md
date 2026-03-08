# DB Lab (Learning-First, from Scratch)

This project now focuses on learning storage/index internals directly in code.

## Implemented pieces

1. Storage Engine Basics
- `SlottedPage`, `Slot`
- `RecordSerializer`

2. Single Table + Sequential Scan
- `InMemoryTable` insert/select APIs

3. Indexes
- `BPlusTree` (search, insert/split, range)
- `BTree` (search, insert/split, range)

4. Clustered vs Secondary
- PK: `pkBPlusIndex` + `pkBTree`
- Secondary: `titleBPlusIndex` storing PK id lists

5. Optimizer mimic
- `QueryEngine` chooses full scan vs secondary index
- `QueryPlan` explain-like output

6. Disk persistence and cache mimic
- `TableSnapshotStore`, `PageFileStore`
- `BufferPool` (LRU cache)

## Minimal UI (REST)

Run app:
```bash
cd /Users/minsang/Desktop/dev/notionlite
./scripts/gradle bootRun
```

Insert row:
```bash
curl -X POST http://localhost:8080/api/lab/rows \
  -H 'content-type: application/json' \
  -d '{"title":"mysql","content":"hello"}'
```

Query by title (with explain plan):
```bash
curl 'http://localhost:8080/api/lab/query/title?value=mysql'
```

See index structures:
```bash
curl 'http://localhost:8080/api/lab/indexes'
```

Range query by primary key:
```bash
curl 'http://localhost:8080/api/lab/query/pk-range?from=1&to=100'
```
