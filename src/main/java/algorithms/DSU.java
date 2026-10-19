package algorithms;

public final class DSU {
    private int[] parent;
    private int[] size;
    private int componentCount;
    private long findCalls;
    private long successfulUnions;

    public DSU(int n) {
        makeSet(n);
    }

    /** Replaces the old partition with n singleton sets and resets counters. */
    public void makeSet(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Set count must be non-negative");
        }
        parent = new int[n];
        size = new int[n];
        for (int vertex = 0; vertex < n; vertex++) {
            parent[vertex] = vertex;
            size[vertex] = 1;
        }
        componentCount = n;
        resetCounters();
    }

    public int find(int vertex) {
        checkVertex(vertex);
        findCalls++;
        int root = vertex;
        while (parent[root] != root) {
            root = parent[root];
        }
        // Compress the entire path without using the JVM call stack.
        while (vertex != root) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    /** Returns true only when two previously different components are joined. */
    public boolean union(int x, int y) {
        checkVertex(x);
        checkVertex(y);
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) {
            return false;
        }
        if (size[rootX] < size[rootY]) {
            int temporary = rootX;
            rootX = rootY;
            rootY = temporary;
        }
        parent[rootY] = rootX;
        size[rootX] += size[rootY];
        componentCount--;
        successfulUnions++;
        return true;
    }

    public int components() {
        return componentCount;
    }

    public long findCalls() {
        return findCalls;
    }

    public long successfulUnions() {
        return successfulUnions;
    }

    public long dsuOps() {
        return findCalls + successfulUnions;
    }

    /** Clears measurements without changing the partition. */
    public void resetCounters() {
        findCalls = 0;
        successfulUnions = 0;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
    }
}
