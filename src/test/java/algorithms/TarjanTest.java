package algorithms;

import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TarjanTest {
    @Test
    void matchesKosarajuOnBothDirectedFilesAndRandomGraphs() throws IOException {
        check(Graph.read(Path.of("data/cyclic-directed.txt")));
        check(Graph.read(Path.of("data/dag-directed.txt")));
        Random random = new Random(98765);
        for (int trial = 0; trial < 100; trial++) {
            int n = random.nextInt(201);
            Graph graph = new Graph(n, true);
            int m = n == 0 ? 0 : random.nextInt(6 * n + 1);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            check(graph);
        }
    }

    @Test
    void handlesEmptyIsolatedParallelAndAlreadyCompletedComponents() {
        check(new Graph(0, true));
        check(new Graph(5, true));
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 2);
        graph.addEdge(2, 0, 3); // Edge into an already completed SCC must not lower lowlink.
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);
        graph.addEdge(3, 2, -5);
        graph.addEdge(4, 4, 0);
        check(graph);
        assertEquals(4, Tarjan.compute(graph).componentCount());
    }

    @Test
    void survivesPathAndCycleOfOneHundredThousandVertices() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 1; vertex < n; vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        Metrics metrics = new Metrics();
        SccResult path = Tarjan.compute(graph, metrics);
        assertEquals(n, path.componentCount());
        assertEquals(n, metrics.maxDepth);
        assertEquals(n - 1, metrics.edgeScans);
        assertTrue(path.samePartitionAs(Kosaraju.compute(graph)));
        graph.addEdge(n - 1, 0, 1);
        SccResult cycle = Tarjan.compute(graph, metrics);
        assertEquals(1, cycle.componentCount());
        assertEquals(n, metrics.maxDepth);
        assertEquals(n, metrics.edgeScans);
        assertTrue(cycle.samePartitionAs(Kosaraju.compute(graph)));
    }

    @Test
    void resetsMetricsAndRejectsUndirectedInput() {
        Metrics metrics = new Metrics();
        metrics.dsuOps = metrics.comparisons = metrics.edgeScans = 99;
        metrics.maxDepth = 99;
        Tarjan.compute(new Graph(2, true), metrics);
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.comparisons);
        assertEquals(0, metrics.edgeScans);
        assertEquals(1, metrics.maxDepth);
        Tarjan.compute(new Graph(0, true), metrics);
        assertEquals(0, metrics.maxDepth);
        assertThrows(IllegalArgumentException.class, () -> Tarjan.compute(new Graph(0, false)));
    }

    @Test
    void partitionComparisonAllowsRelabelingButDetectsDifferentMembership() {
        SccResult first = new SccResult(new int[]{0, 0, 1, 2}, 3);
        assertTrue(first.samePartitionAs(new SccResult(new int[]{2, 2, 0, 1}, 3)));
        assertFalse(first.samePartitionAs(new SccResult(new int[]{0, 1, 1, 2}, 3)));
        assertFalse(first.samePartitionAs(new SccResult(new int[]{0, 0, 1, 1}, 2)));
        assertFalse(first.samePartitionAs(new SccResult(new int[]{0}, 1)));
        assertFalse(first.samePartitionAs(null));
        assertTrue(new SccResult(new int[0], 0).samePartitionAs(new SccResult(new int[0], 0)));
    }

    private static void check(Graph graph) {
        Metrics metrics = new Metrics();
        SccResult actual = Tarjan.compute(graph, metrics);
        SccResult expected = Kosaraju.compute(graph);
        assertTrue(expected.samePartitionAs(actual));
        for (int u = 0; u < graph.vertexCount(); u++) {
            for (int v = 0; v < graph.vertexCount(); v++) {
                assertEquals(expected.componentOf(u) == expected.componentOf(v),
                        actual.componentOf(u) == actual.componentOf(v));
            }
        }
        assertEquals(graph.edgeCount(), metrics.edgeScans);
        assertTrue(metrics.maxDepth <= graph.vertexCount());
    }
}
