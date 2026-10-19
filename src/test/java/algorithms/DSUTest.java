package algorithms;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DSUTest {
    @Test
    void startsWithSingletonSets() {
        DSU dsu = new DSU(5);
        assertEquals(5, dsu.components());
        assertEquals(0, dsu.dsuOps());
        for (int vertex = 0; vertex < 5; vertex++) {
            assertEquals(vertex, dsu.find(vertex));
        }
        assertEquals(5, dsu.findCalls());
    }

    @Test
    void handlesEmptyAndSingletonSets() {
        DSU empty = new DSU(0);
        assertEquals(0, empty.components());
        assertThrows(IllegalArgumentException.class, () -> empty.find(0));
        assertThrows(IllegalArgumentException.class, () -> empty.union(0, 0));
        assertEquals(0, empty.dsuOps());
        DSU singleton = new DSU(1);
        assertFalse(singleton.union(0, 0));
        assertEquals(1, singleton.components());
        assertEquals(0, singleton.find(0));
    }

    @Test
    void countsFindCallsAndOnlySuccessfulUnions() {
        DSU dsu = new DSU(3);
        dsu.find(0);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertFalse(dsu.union(2, 2));
        assertEquals(7, dsu.findCalls());
        assertEquals(1, dsu.successfulUnions());
        assertEquals(8, dsu.dsuOps());
        assertEquals(2, dsu.components());
        assertTrue(dsu.union(1, 2));
        assertEquals(9, dsu.findCalls());
        assertEquals(2, dsu.successfulUnions());
        assertEquals(11, dsu.dsuOps());
        assertEquals(1, dsu.components());
    }

    @Test
    void resetCountersPreservesPartition() {
        DSU dsu = new DSU(3);
        dsu.union(0, 1);
        dsu.resetCounters();
        assertEquals(0, dsu.findCalls());
        assertEquals(0, dsu.successfulUnions());
        assertEquals(0, dsu.dsuOps());
        assertEquals(2, dsu.components());
        assertEquals(dsu.find(0), dsu.find(1));
        assertNotEquals(dsu.find(0), dsu.find(2));
    }

    @Test
    void makeSetReplacesPartitionAndResetsCounters() {
        DSU dsu = new DSU(4);
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.makeSet(2);
        assertEquals(2, dsu.components());
        assertEquals(0, dsu.dsuOps());
        assertNotEquals(dsu.find(0), dsu.find(1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(2));
        dsu.makeSet(0);
        assertEquals(0, dsu.components());
        assertEquals(0, dsu.dsuOps());
    }

    @Test
    void rejectsInvalidArgumentsWithoutChangingStateOrCounters() {
        assertThrows(IllegalArgumentException.class, () -> new DSU(-1));
        DSU dsu = new DSU(2);
        dsu.union(0, 1);
        long operations = dsu.dsuOps();
        assertThrows(IllegalArgumentException.class, () -> dsu.find(-1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(2));
        assertThrows(IllegalArgumentException.class, () -> dsu.union(0, 2));
        assertThrows(IllegalArgumentException.class, () -> dsu.union(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> dsu.makeSet(-1));
        assertEquals(operations, dsu.dsuOps());
        assertEquals(1, dsu.components());
        assertEquals(dsu.find(0), dsu.find(1));
    }

    @Test
    void matchesSimpleReferenceAfterEveryRandomUnion() {
        int[] sizes = {1, 2, 16, 50};
        for (int n : sizes) {
            Random random = new Random(42 + n);
            DSU dsu = new DSU(n);
            ReferenceSets reference = new ReferenceSets(n);
            for (int step = 0; step < 300; step++) {
                int x = random.nextInt(n);
                int y = random.nextInt(n);
                assertEquals(reference.union(x, y), dsu.union(x, y));
                assertEquals(reference.components(), dsu.components());
                int[] roots = new int[n];
                for (int vertex = 0; vertex < n; vertex++) {
                    roots[vertex] = dsu.find(vertex);
                }
                // Representatives may differ; compare membership, not root numbers.
                for (int u = 0; u < n; u++) {
                    for (int v = 0; v < n; v++) {
                        assertEquals(reference.connected(u, v), roots[u] == roots[v]);
                    }
                }
            }
        }
    }

    @Test
    void handlesOneHundredThousandElements() {
        int n = 100_000;
        DSU dsu = new DSU(n);
        for (int vertex = 1; vertex < n; vertex++) {
            assertTrue(dsu.union(vertex, vertex - 1));
        }
        assertEquals(1, dsu.components());
        assertEquals(n - 1, dsu.successfulUnions());
        int root = dsu.find(0);
        for (int vertex = 1; vertex < n; vertex++) {
            assertEquals(root, dsu.find(vertex));
        }
    }

    /** Deliberately simple: a union relabels every member of one set. */
    private static final class ReferenceSets {
        private final int[] label;

        private ReferenceSets(int n) {
            label = new int[n];
            for (int vertex = 0; vertex < n; vertex++) {
                label[vertex] = vertex;
            }
        }

        private boolean connected(int x, int y) {
            return label[x] == label[y];
        }

        private boolean union(int x, int y) {
            if (connected(x, y)) {
                return false;
            }
            int oldLabel = label[y];
            int newLabel = label[x];
            for (int vertex = 0; vertex < label.length; vertex++) {
                if (label[vertex] == oldLabel) {
                    label[vertex] = newLabel;
                }
            }
            return true;
        }

        private int components() {
            boolean[] seen = new boolean[label.length];
            int count = 0;
            for (int value : label) {
                if (!seen[value]) {
                    seen[value] = true;
                    count++;
                }
            }
            return count;
        }
    }
}
