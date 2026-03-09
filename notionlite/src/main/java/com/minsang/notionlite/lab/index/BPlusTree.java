package com.minsang.notionlite.lab.index;

// 리스트, 큐, Optional 등 자료구조 유틸리티를 사용합니다.
import java.util.*;

/**
 * 학습용 B+Tree 구현입니다.
 *
 * 지원 기능:
 * - 단건 검색
 * - 분할(split)을 동반한 삽입
 * - 리프 노드 연결을 이용한 범위 검색
 *
 * 일부러 아직 넣지 않은 기능:
 * - 삭제
 * - 병합
 * - 재균형
 */
public class BPlusTree<K extends Comparable<K>, V> {
    // 한 노드가 가질 수 있는 최대 key 개수입니다.
    private final int maxKeys;
    // 트리의 시작점입니다.
    private Node root;

    public BPlusTree(int maxKeys) {
        // 너무 작은 차수는 트리 학습용으로도 의미가 약하므로 제한합니다.
        if (maxKeys < 3) {
            throw new IllegalArgumentException("maxKeys must be >= 3");
        }
        this.maxKeys = maxKeys;
        // 처음에는 비어 있는 리프 노드 하나만 루트로 둡니다.
        this.root = new LeafNode();
    }

    public Optional<V> search(K key) {
        // 먼저 key가 들어 있을 리프 노드를 찾습니다.
        LeafNode leaf = findLeaf(key);
        // 리프 내부에서는 정렬된 배열이므로 이진 탐색을 합니다.
        int index = Collections.binarySearch(leaf.keys, key);
        if (index < 0) {
            return Optional.empty();
        }
        // 같은 위치의 value를 반환합니다.
        return Optional.of(leaf.values.get(index));
    }

    public void insert(K key, V value) {
        // 재귀 삽입 결과로 split이 발생하면 부모가 받아서 처리해야 합니다.
        Split split = insertRecursive(root, key, value);
        if (split == null) {
            return;
        }

        // 루트까지 쪼개졌다면 트리 높이를 하나 올려 새 루트를 만듭니다.
        InternalNode newRoot = new InternalNode();
        newRoot.keys.add(split.separator);
        newRoot.children.add(root);
        newRoot.children.add(split.rightNode);
        root = newRoot;
    }

    public List<Map.Entry<K, V>> rangeSearch(K fromInclusive, K toInclusive) {
        // 잘못된 범위면 빈 결과를 돌려줍니다.
        if (fromInclusive.compareTo(toInclusive) > 0) {
            return List.of();
        }

        List<Map.Entry<K, V>> rows = new ArrayList<>();
        // 시작 key가 들어 있을 리프부터 읽기 시작합니다.
        LeafNode leaf = findLeaf(fromInclusive);

        while (leaf != null) {
            for (int i = 0; i < leaf.keys.size(); i++) {
                K key = leaf.keys.get(i);
                // 시작 범위보다 작은 값은 건너뜁니다.
                if (key.compareTo(fromInclusive) < 0) {
                    continue;
                }
                // 끝 범위를 넘으면 이후 리프도 더 볼 필요가 없습니다.
                if (key.compareTo(toInclusive) > 0) {
                    return rows;
                }
                rows.add(Map.entry(key, leaf.values.get(i)));
            }
            // B+Tree는 리프끼리 연결되어 있어 다음 리프로 바로 이동할 수 있습니다.
            leaf = leaf.next;
        }

        return rows;
    }

    public String debugStructure() {
        // 레벨 순회(BFS)로 트리 구조를 사람이 보기 쉽게 문자열로 만듭니다.
        StringBuilder sb = new StringBuilder();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(root);
        int level = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            sb.append("L").append(level).append(": ");
            for (int i = 0; i < size; i++) {
                Node node = queue.poll();
                sb.append(node.keys);
                if (node instanceof InternalNode internal) {
                    // 내부 노드는 자식들을 다음 레벨에 추가합니다.
                    queue.addAll(internal.children);
                }
                if (i < size - 1) {
                    sb.append(" | ");
                }
            }
            sb.append("\n");
            level++;
        }
        return sb.toString();
    }

    private Split insertRecursive(Node node, K key, V value) {
        // 리프에 도달하면 실제 삽입을 수행합니다.
        if (node instanceof LeafNode leaf) {
            return insertIntoLeaf(leaf, key, value);
        }

        // 내부 노드라면 어느 자식으로 내려갈지 먼저 계산합니다.
        InternalNode internal = (InternalNode) node;
        int childIdx = childIndex(internal.keys, key);
        Split childSplit = insertRecursive(internal.children.get(childIdx), key, value);
        if (childSplit == null) {
            return null;
        }

        // 자식이 쪼개졌다면 separator key와 새 오른쪽 노드를 현재 노드에 반영합니다.
        internal.keys.add(childIdx, childSplit.separator);
        internal.children.add(childIdx + 1, childSplit.rightNode);

        // 아직 용량 안이면 split을 더 전파하지 않습니다.
        if (internal.keys.size() <= maxKeys) {
            return null;
        }

        // 현재 내부 노드도 가득 찼다면 다시 분할합니다.
        return splitInternal(internal);
    }

    private Split insertIntoLeaf(LeafNode leaf, K key, V value) {
        // 리프의 key 배열은 항상 정렬 상태를 유지합니다.
        int idx = Collections.binarySearch(leaf.keys, key);
        if (idx >= 0) {
            // 이미 key가 있으면 값을 덮어써서 upsert처럼 동작하게 합니다.
            leaf.values.set(idx, value);
            return null;
        }

        // binarySearch의 음수 결과를 실제 삽입 위치로 바꿉니다.
        int insertAt = -idx - 1;
        leaf.keys.add(insertAt, key);
        leaf.values.add(insertAt, value);

        // 아직 여유가 있으면 split 없이 끝납니다.
        if (leaf.keys.size() <= maxKeys) {
            return null;
        }

        // 용량 초과 시 리프를 둘로 나눕니다.
        return splitLeaf(leaf);
    }

    private Split splitLeaf(LeafNode left) {
        // 가운데를 기준으로 왼쪽/오른쪽 리프로 나눕니다.
        int mid = left.keys.size() / 2;

        LeafNode right = new LeafNode();
        right.keys.addAll(left.keys.subList(mid, left.keys.size()));
        right.values.addAll(left.values.subList(mid, left.values.size()));

        // 오른쪽으로 넘긴 데이터는 왼쪽에서 제거합니다.
        left.keys.subList(mid, left.keys.size()).clear();
        left.values.subList(mid, left.values.size()).clear();

        // 리프 연결 리스트도 유지해야 범위 검색이 가능합니다.
        right.next = left.next;
        left.next = right;

        // B+Tree에서는 오른쪽 첫 key가 부모로 올라갈 separator가 됩니다.
        return new Split(right.keys.get(0), right);
    }

    private Split splitInternal(InternalNode left) {
        // 내부 노드는 가운데 key 하나를 부모에게 올립니다.
        int mid = left.keys.size() / 2;
        K separator = left.keys.get(mid);

        InternalNode right = new InternalNode();
        right.keys.addAll(left.keys.subList(mid + 1, left.keys.size()));
        right.children.addAll(left.children.subList(mid + 1, left.children.size()));

        // separator 이후 데이터만 오른쪽에 남기고 왼쪽은 잘라냅니다.
        left.keys.subList(mid, left.keys.size()).clear();
        left.children.subList(mid + 1, left.children.size()).clear();

        return new Split(separator, right);
    }

    private LeafNode findLeaf(K key) {
        // 루트부터 시작해서 리프에 도달할 때까지 아래로 내려갑니다.
        Node node = root;
        while (node instanceof InternalNode internal) {
            node = internal.children.get(childIndex(internal.keys, key));
        }
        return (LeafNode) node;
    }

    private int childIndex(List<K> keys, K key) {
        // 내부 노드에서 어느 자식 포인터를 따라갈지 이진 탐색 방식으로 계산합니다.
        int low = 0;
        int high = keys.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (key.compareTo(keys.get(mid)) < 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private abstract class Node {
        // 각 노드는 자신의 key 목록을 가집니다.
        protected final List<K> keys = new ArrayList<>();
    }

    private final class InternalNode extends Node {
        // 내부 노드는 자식 포인터들을 가집니다.
        private final List<Node> children = new ArrayList<>();
    }

    private final class LeafNode extends Node {
        // 리프 노드는 실제 value를 저장합니다.
        private final List<V> values = new ArrayList<>();
        // 다음 리프를 가리켜 범위 검색을 빠르게 만듭니다.
        private LeafNode next;
    }

    private final class Split {
        // 부모에게 올려줄 경계 key입니다.
        private final K separator;
        // 분할 후 새로 생긴 오른쪽 노드입니다.
        private final Node rightNode;

        private Split(K separator, Node rightNode) {
            this.separator = separator;
            this.rightNode = rightNode;
        }
    }
}
