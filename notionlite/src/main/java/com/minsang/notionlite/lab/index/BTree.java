package com.minsang.notionlite.lab.index;

import java.util.*;

/**
 * Learning-focused classic B-Tree (not B+Tree).
 *
 * Differences from B+Tree:
 * - Data can live in internal nodes as well.
 * - No linked leaf chain for range scan.
 */
public class BTree<K extends Comparable<K>, V> {
    private final int minDegree;
    private Node root;

    public BTree(int minDegree) {
        if (minDegree < 2) {
            throw new IllegalArgumentException("minDegree must be >= 2");
        }
        this.minDegree = minDegree;
        this.root = new Node(true);
    }

    public Optional<V> search(K key) {
        return search(root, key);
    }

    public void insert(K key, V value) {
        Node oldRoot = root;

        if (oldRoot.keys.size() == maxKeys()) {
            Node newRoot = new Node(false);
            newRoot.children.add(oldRoot);
            splitChild(newRoot, 0);
            root = newRoot;
        }

        insertNonFull(root, key, value);
    }

    public List<Map.Entry<K, V>> rangeSearch(K fromInclusive, K toInclusive) {
        List<Map.Entry<K, V>> out = new ArrayList<>();
        traverseRange(root, fromInclusive, toInclusive, out);
        return out;
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
                if (!node.leaf) {
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
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }

        if (i < node.keys.size() && key.compareTo(node.keys.get(i)) == 0) {
            return Optional.of(node.values.get(i));
        }

        if (node.leaf) {
            return Optional.empty();
        }

        return search(node.children.get(i), key);
    }

    private void insertNonFull(Node node, K key, V value) {
        int i = node.keys.size() - 1;

        if (node.leaf) {
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }

            int insertAt = i + 1;
            if (insertAt < node.keys.size() && key.compareTo(node.keys.get(insertAt)) == 0) {
                node.values.set(insertAt, value);
                return;
            }

            node.keys.add(insertAt, key);
            node.values.add(insertAt, value);
            return;
        }

        while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
            i--;
        }
        int childIndex = i + 1;

        Node child = node.children.get(childIndex);
        if (child.keys.size() == maxKeys()) {
            splitChild(node, childIndex);

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
        Node full = parent.children.get(childIndex);
        Node right = new Node(full.leaf);

        int mid = minDegree - 1;

        K promotedKey = full.keys.get(mid);
        V promotedValue = full.values.get(mid);

        // Move keys/values to right sibling.
        right.keys.addAll(full.keys.subList(mid + 1, full.keys.size()));
        right.values.addAll(full.values.subList(mid + 1, full.values.size()));

        // Truncate left sibling.
        full.keys.subList(mid, full.keys.size()).clear();
        full.values.subList(mid, full.values.size()).clear();

        // Move children if internal node.
        if (!full.leaf) {
            right.children.addAll(full.children.subList(minDegree, full.children.size()));
            full.children.subList(minDegree, full.children.size()).clear();
        }

        parent.keys.add(childIndex, promotedKey);
        parent.values.add(childIndex, promotedValue);
        parent.children.add(childIndex + 1, right);
    }

    private void traverseRange(Node node, K fromInclusive, K toInclusive, List<Map.Entry<K, V>> out) {
        int i = 0;

        while (i < node.keys.size()) {
            if (!node.leaf) {
                traverseRange(node.children.get(i), fromInclusive, toInclusive, out);
            }

            K key = node.keys.get(i);
            if (key.compareTo(fromInclusive) >= 0 && key.compareTo(toInclusive) <= 0) {
                out.add(Map.entry(key, node.values.get(i)));
            }
            i++;
        }

        if (!node.leaf) {
            traverseRange(node.children.get(i), fromInclusive, toInclusive, out);
        }
    }

    private int maxKeys() {
        return minDegree * 2 - 1;
    }

    private final class Node {
        private final boolean leaf;
        private final List<K> keys = new ArrayList<>();
        private final List<V> values = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();

        private Node(boolean leaf) {
            this.leaf = leaf;
        }
    }
}
