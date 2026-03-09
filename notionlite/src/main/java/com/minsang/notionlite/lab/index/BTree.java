package com.minsang.notionlite.lab.index;

// 여러 컬렉션 유틸리티를 사용합니다.
import java.util.*;

/**
 * 학습용 클래식 B-Tree 구현입니다.
 *
 * B+Tree와의 차이:
 * - 내부 노드에도 값이 들어갈 수 있습니다.
 * - 리프 연결 리스트가 없어서 범위 검색이 덜 효율적일 수 있습니다.
 */
public class BTree<K extends Comparable<K>, V> {
    // 최소 차수입니다. B-Tree의 최대/최소 key 수 계산 기준이 됩니다.
    private final int minDegree;
    // 트리의 시작 노드입니다.
    private Node root;

    public BTree(int minDegree) {
        // 최소 차수 2부터 제대로 된 B-Tree 형태가 됩니다.
        if (minDegree < 2) {
            throw new IllegalArgumentException("minDegree must be >= 2");
        }
        this.minDegree = minDegree;
        // 처음 루트는 비어 있는 리프 노드입니다.
        this.root = new Node(true);
    }

    public Optional<V> search(K key) {
        return search(root, key);
    }

    public void insert(K key, V value) {
        // 루트가 가득 차 있으면 높이를 올리고 split부터 수행합니다.
        Node oldRoot = root;

        if (oldRoot.keys.size() == maxKeys()) {
            Node newRoot = new Node(false);
            newRoot.children.add(oldRoot);
            splitChild(newRoot, 0);
            root = newRoot;
        }

        // 이제 "가득 차지 않은 노드"로 내려가며 삽입할 수 있습니다.
        insertNonFull(root, key, value);
    }

    public List<Map.Entry<K, V>> rangeSearch(K fromInclusive, K toInclusive) {
        // B-Tree는 리프 연결이 없으므로 재귀 순회로 범위를 찾습니다.
        List<Map.Entry<K, V>> out = new ArrayList<>();
        traverseRange(root, fromInclusive, toInclusive, out);
        return out;
    }

    public String debugStructure() {
        // 레벨별로 key 분포를 보여주면 split 결과를 이해하기 쉽습니다.
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
                if (!node.leaf) {
                    // 내부 노드라면 자식들을 다음 레벨로 넘깁니다.
                    queue.addAll(node.children);
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

    private Optional<V> search(Node node, K key) {
        // 현재 노드에서 key가 들어갈 위치를 찾습니다.
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }

        // 같은 key를 찾았으면 이 노드의 값을 반환합니다.
        if (i < node.keys.size() && key.compareTo(node.keys.get(i)) == 0) {
            return Optional.of(node.values.get(i));
        }

        // 리프인데 못 찾았으면 검색 실패입니다.
        if (node.leaf) {
            return Optional.empty();
        }

        // 내부 노드면 적절한 자식으로 내려가 계속 찾습니다.
        return search(node.children.get(i), key);
    }

    private void insertNonFull(Node node, K key, V value) {
        // 뒤에서부터 비교하면 삽입 위치를 찾기 쉽습니다.
        int i = node.keys.size() - 1;

        if (node.leaf) {
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }

            int insertAt = i + 1;
            // 같은 key가 이미 있으면 값을 덮어씁니다.
            if (insertAt < node.keys.size() && key.compareTo(node.keys.get(insertAt)) == 0) {
                node.values.set(insertAt, value);
                return;
            }

            // 리프라면 해당 위치에 직접 key/value를 삽입합니다.
            node.keys.add(insertAt, key);
            node.values.add(insertAt, value);
            return;
        }

        // 내부 노드면 어느 자식으로 내려갈지 결정합니다.
        while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
            i--;
        }
        int childIndex = i + 1;

        Node child = node.children.get(childIndex);
        // 내려가기 전에 자식이 꽉 찼으면 미리 split합니다.
        if (child.keys.size() == maxKeys()) {
            splitChild(node, childIndex);

            // split 이후엔 가운데 key와 비교해 어느 쪽 자식으로 갈지 다시 판단합니다.
            int cmp = key.compareTo(node.keys.get(childIndex));
            if (cmp > 0) {
                childIndex++;
            } else if (cmp == 0) {
                node.values.set(childIndex, value);
                return;
            }
        }

        insertNonFull(node.children.get(childIndex), key, value);
    }

    private void splitChild(Node parent, int childIndex) {
        // 가득 찬 자식을 왼쪽(full)과 오른쪽(right)으로 나눕니다.
        Node full = parent.children.get(childIndex);
        Node right = new Node(full.leaf);

        // 가운데 원소가 부모로 승격됩니다.
        int mid = minDegree - 1;

        K promotedKey = full.keys.get(mid);
        V promotedValue = full.values.get(mid);

        // 가운데 다음 key/value들은 오른쪽 형제로 이동합니다.
        right.keys.addAll(full.keys.subList(mid + 1, full.keys.size()));
        right.values.addAll(full.values.subList(mid + 1, full.values.size()));

        // 왼쪽 노드에는 가운데 이전까지만 남깁니다.
        full.keys.subList(mid, full.keys.size()).clear();
        full.values.subList(mid, full.values.size()).clear();

        // 내부 노드였다면 자식 포인터도 절반을 오른쪽으로 옮겨야 합니다.
        if (!full.leaf) {
            right.children.addAll(full.children.subList(minDegree, full.children.size()));
            full.children.subList(minDegree, full.children.size()).clear();
        }

        // 부모 노드에 승격 key와 새 오른쪽 자식을 연결합니다.
        parent.keys.add(childIndex, promotedKey);
        parent.values.add(childIndex, promotedValue);
        parent.children.add(childIndex + 1, right);
    }

    private void traverseRange(Node node, K fromInclusive, K toInclusive, List<Map.Entry<K, V>> out) {
        // 중위 순회 비슷하게 방문하면 정렬 순서대로 결과를 얻을 수 있습니다.
        int i = 0;

        while (i < node.keys.size()) {
            if (!node.leaf) {
                traverseRange(node.children.get(i), fromInclusive, toInclusive, out);
            }

            K key = node.keys.get(i);
            // 현재 key가 범위 안에 있으면 결과에 추가합니다.
            if (key.compareTo(fromInclusive) >= 0 && key.compareTo(toInclusive) <= 0) {
                out.add(Map.entry(key, node.values.get(i)));
            }
            i++;
        }

        // 마지막 key 오른쪽 자식도 잊지 않고 순회합니다.
        if (!node.leaf) {
            traverseRange(node.children.get(i), fromInclusive, toInclusive, out);
        }
    }

    private int maxKeys() {
        // B-Tree 규칙상 최대 key 수는 2t - 1 입니다.
        return minDegree * 2 - 1;
    }

    private final class Node {
        // 리프 여부에 따라 자식 유무가 결정됩니다.
        private final boolean leaf;
        // 현재 노드의 정렬된 key 목록입니다.
        private final List<K> keys = new ArrayList<>();
        // 각 key에 대응하는 값입니다.
        private final List<V> values = new ArrayList<>();
        // 내부 노드일 때만 의미가 있는 자식 포인터들입니다.
        private final List<Node> children = new ArrayList<>();

        private Node(boolean leaf) {
            this.leaf = leaf;
        }
    }
}
