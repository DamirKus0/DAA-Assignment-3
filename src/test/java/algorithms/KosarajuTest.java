package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KosarajuTest {
    @Test
    void findsKnownComponentsFromFile() throws IOException {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        SccResult result = Kosaraju.compute(graph);
        assertEquals(3, result.componentCount());
        assertEquals(result.componentOf(0), result.componentOf(1));
        assertEquals(result.componentOf(1), result.componentOf(2));
        assertEquals(result.componentOf(3), result.componentOf(4));
        assertNotEquals(result.componentOf(2), result.componentOf(3));
        assertNotEquals(result.componentOf(0), result.componentOf(5));
        assertNotEquals(result.componentOf(3), result.componentOf(5));
        assertMatchesReachability(graph, result);
    }

    @Test
    void handlesEmptyGraphAndIsolates() {
        for (int n : new int[]{0, 1, 5}) {
            Graph graph = new Graph(n, true);
            Metrics metrics = new Metrics();
            SccResult result = Kosaraju.compute(graph, metrics);
            assertEquals(n, result.componentCount());
            assertEquals(n == 0 ? 0 : 1, metrics.maxDepth);
            assertEquals(0, metrics.edgeScans);
            assertMatchesReachability(graph, result);
        }
    }

    @Test
    void handlesParallelEdgesLoopsAndIgnoredWeights() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 0, Integer.MIN_VALUE);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 1, -5);
        graph.addEdge(1, 0, Integer.MAX_VALUE);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 2, -1);
        Metrics metrics = new Metrics();
        SccResult result = Kosaraju.compute(graph, metrics);
        assertEquals(3, result.componentCount());
        assertEquals(12, metrics.edgeScans);
        assertMatchesReachability(graph, result);
    }

    @Test
    void usesFinishOrderRatherThanDiscoveryOrder() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 1);
        SccResult result = Kosaraju.compute(graph);
        assertEquals(3, result.componentCount());
        assertMatchesReachability(graph, result);
    }

    @Test
    void everyDagVertexIsItsOwnComponent() throws IOException {
        Graph graph = Graph.read(Path.of("data/dag-directed.txt"));
        SccResult result = Kosaraju.compute(graph);
        assertEquals(graph.vertexCount(), result.componentCount());
        assertMatchesReachability(graph, result);
    }

    @Test
    void matchesBfsMutualReachabilityOnOneHundredRandomGraphs() {
        Random random = new Random(314159);
        for (int trial = 0; trial < 100; trial++) {
            int n = trial % 10 == 0 ? 200 : random.nextInt(201);
            int m = n == 0 ? 0 : random.nextInt(6 * n + 1);
            Graph graph = new Graph(n, true);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            Metrics metrics = new Metrics();
            SccResult result = Kosaraju.compute(graph, metrics);
            assertMatchesReachability(graph, result);
            assertEquals(2L * m, metrics.edgeScans);
            assertTrue(metrics.maxDepth >= (n == 0 ? 0 : 1));
            assertTrue(metrics.maxDepth <= n);
        }
    }

    @Test
    void handlesPathOfOneHundredThousandVerticesWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 1; vertex < n; vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        Metrics metrics = new Metrics();
        SccResult result = Kosaraju.compute(graph, metrics);
        assertEquals(n, result.componentCount());
        boolean[] seen = new boolean[n];
        for (int vertex = 0; vertex < n; vertex++) {
            int component = result.componentOf(vertex);
            assertTrue(component >= 0 && component < n);
            assertFalse(seen[component]);
            seen[component] = true;
        }
        assertEquals(n, metrics.maxDepth);
        assertEquals(2L * (n - 1), metrics.edgeScans);
    }

    @Test
    void bothPassesHandleCycleOfOneHundredThousandVertices() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 0; vertex < n; vertex++) {
            graph.addEdge(vertex, (vertex + 1) % n, 1);
        }
        Metrics metrics = new Metrics();
        SccResult result = Kosaraju.compute(graph, metrics);
        assertEquals(1, result.componentCount());
        for (int vertex = 0; vertex < n; vertex++) {
            assertEquals(0, result.componentOf(vertex));
        }
        assertEquals(n, metrics.maxDepth);
        assertEquals(2L * n, metrics.edgeScans);
    }

    @Test
    void includesSecondPassInMaximumStackDepth() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(0, 3, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(2, 1, 1);
        graph.addEdge(3, 2, 1);
        Metrics metrics = new Metrics();
        SccResult result = Kosaraju.compute(graph, metrics);
        assertEquals(1, result.componentCount());
        // First pass has depth 2; reversed DFS follows 0 -> 1 -> 2 -> 3.
        assertEquals(4, metrics.maxDepth);
    }

    @Test
    void resetsMetricsAndDoesNotReverseOriginalGraphInPlace() throws IOException {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        Edge first = graph.edges().get(0);
        Metrics metrics = new Metrics();
        SccResult firstResult = Kosaraju.compute(graph, metrics);
        int depth = metrics.maxDepth;
        metrics.dsuOps = 99;
        metrics.comparisons = 99;
        SccResult secondResult = Kosaraju.compute(graph, metrics);
        assertArrayEquals(firstResult.componentIds(), secondResult.componentIds());
        assertEquals(14, metrics.edgeScans);
        assertEquals(depth, metrics.maxDepth);
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.comparisons);
        assertSame(first, graph.edges().get(0));
        assertEquals(0, first.from);
        assertEquals(1, first.to);
        assertEquals(7, graph.edgeCount());
        Kosaraju.compute(new Graph(0, true), metrics);
        assertEquals(0, metrics.edgeScans);
        assertEquals(0, metrics.maxDepth);
    }

    @Test
    void resultProtectsComponentArrayAndChecksVertexBounds() {
        SccResult result = Kosaraju.compute(new Graph(2, true));
        int originalId = result.componentOf(0);
        int[] copy = result.componentIds();
        copy[0] = -100;
        assertEquals(originalId, result.componentOf(0));
        assertThrows(IllegalArgumentException.class, () -> result.componentOf(-1));
        assertThrows(IllegalArgumentException.class, () -> result.componentOf(2));
    }

    @Test
    void rejectsUndirectedGraphs() throws IOException {
        Graph input = Graph.read(Path.of("data/disconnected-undirected.txt"));
        for (Graph graph : new Graph[]{input, new Graph(0, false)}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> Kosaraju.compute(graph));
            assertTrue(error.getMessage().contains("directed"));
        }
    }

    private static void assertMatchesReachability(Graph graph, SccResult result) {
        int n = graph.vertexCount();
        boolean[][] reachable = new boolean[n][n];
        for (int start = 0; start < n; start++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            reachable[start][start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Edge edge : graph.neighbors(vertex)) {
                    if (!reachable[start][edge.to]) {
                        reachable[start][edge.to] = true;
                        queue.add(edge.to);
                    }
                }
            }
        }
        assertEquals(n, result.vertexCount());
        boolean[] seenIds = new boolean[result.componentCount()];
        int expectedCount = 0;
        for (int u = 0; u < n; u++) {
            int component = result.componentOf(u);
            assertTrue(component >= 0 && component < result.componentCount());
            seenIds[component] = true;
            boolean smallestInComponent = true;
            for (int v = 0; v < n; v++) {
                boolean sameComponent = reachable[u][v] && reachable[v][u];
                assertEquals(sameComponent, component == result.componentOf(v));
                if (v < u && sameComponent) {
                    smallestInComponent = false;
                }
            }
            if (smallestInComponent) {
                expectedCount++;
            }
        }
        assertEquals(expectedCount, result.componentCount());
        for (boolean seen : seenIds) {
            assertTrue(seen);
        }
    }
}
