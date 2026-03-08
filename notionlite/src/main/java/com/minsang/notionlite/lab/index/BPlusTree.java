package com.minsang.notionlite.lab.index;

import java.util.*;

/**
 * Learning-focused B+Tree.
 *
 * Supported:
 * - point search
 * - insert with split
 * - range search via leaf linked list
 *
 * Not supported (intentionally postponed):
 * - delete/merge/rebalance
 */
public class BPlusTree<K extends Comparable<K>, V> {
    private final int maxKeys;
    private Node root;

    public BPlusTree(int maxKeys) {
        if (maxKeys < 3) {
            throw new IllegalArgumentException("maxKeys must be >= 3");
        }
        this.maxKeys = maxKeys;
        this.root = new LeafNode();
    }

    public Optional<V> search(K key) {
        LeafNode leaf = findLeaf(key);
        int index = Collections.binarySearch(leaf.keys, key);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(leaf.values.get(index));
    }

    public void insert(K key, V value) {
        Split split = insertRecursive(root, key, value);
        if (split == null) {
            return;
        }

        InternalNode newRoot = new InternalNode();
        newRoot.keys.add(split.separator);
        newRoot.children.add(root);
        newRoot.children.add(split.rightNode);
        root = newRoot;
    }

    public List<Map.Entry<K, V>> rangeSearch(K fromInclusive, K toInclusive) {
        if (fromInclusive.compareTo(toInclusive) > 0) {
            return List.of();
        }

        List<Map.Entry<K, V>> rows = new ArrayList<>();
        LeafNode leaf = findLeaf(fromInclusive);

        while (leaf != null) {
            for (int i = 0; i < leaf.keys.size(); i++) {
                K key = leaf.keys.get(i);
                if (key.compareTo(fromInclusive) < 0) {
                    continue;
                }
                if (key.compareTo(toInclusive) > 0) {
                    return rows;
                }
                rows.add(Map.entry(key, leaf.values.get(i)));
            }
            leaf = leaf.next;
        }

        return rows;
    }

    public String debugStructure() {
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
        if (node instanceof LeafNode leaf) {
            return insertIntoLeaf(leaf, key, value);
        }

        InternalNode internal = (InternalNode) node;
        int childIdx = childIndex(internal.keys, key);
        Split childSplit = insertRecursive(internal.children.get(childIdx), key, value);
        if (childSplit == null) {
            return null;
        }

        internal.keys.add(childIdx, childSplit.separator);
        internal.children.add(childIdx + 1, childSplit.rightNode);

        if (internal.keys.size() <= maxKeys) {
            return null;
        }

        return splitInternal(internal);
    }

    private Split insertIntoLeaf(LeafNode leaf, K key, V value) {
        int idx = Collections.binarySearch(leaf.keys, key);
        if (idx >= 0) {
            // Upsert behavior makes secondary-index list updates straightforward.
            leaf.values.set(idx, value);
            return null;
        }

        int insertAt = -idx - 1;
        leaf.keys.add(insertAt, key);
        leaf.values.add(insertAt, value);

        if (leaf.keys.size() <= maxKeys) {
            return null;
        }

        return splitLeaf(leaf);
    }

    private Split splitLeaf(LeafNode left) {
        int mid = left.keys.size() / 2;

        LeafNode right = new LeafNode();
        right.keys.addAll(left.keys.subList(mid, left.keys.size()));
        right.values.addAll(left.values.subList(mid, left.values.size()));

        left.keys.subList(mid, left.keys.size()).clear();
        left.values.subList(mid, left.values.size()).clear();

        right.next = left.next;
        left.next = right;

        return new Split(right.keys.get(0), right);
    }

    private Split splitInternal(InternalNode left) {
        int mid = left.keys.size() / 2;
        K separator = left.keys.get(mid);

        InternalNode right = new InternalNode();
        right.keys.addAll(left.keys.subList(mid + 1, left.keys.size()));
        right.children.addAll(left.children.subList(mid + 1, left.children.size()));

        left.keys.subList(mid, left.keys.size()).clear();
        left.children.subList(mid + 1, left.children.size()).clear();

        return new Split(separator, right);
    }

    private LeafNode findLeaf(K key) {
        Node node = root;
        while (node instanceof InternalNode internal) {
            node = internal.children.get(childIndex(internal.keys, key));
        }
        return (LeafNode) node;
    }

    private int childIndex(List<K> keys, K key) {
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
        protected final List<K> keys = new ArrayList<>();
    }

    private final class InternalNode extends Node {
        private final List<Node> children = new ArrayList<>();
    }

    private final class LeafNode extends Node {
        private final List<V> values = new ArrayList<>();
        private LeafNode next;
    }

    private final class Split {
        private final K separator;
        private final Node rightNode;

        private Split(K separator, Node rightNode) {
            this.separator = separator;
            this.rightNode = rightNode;
        }
    }
}
