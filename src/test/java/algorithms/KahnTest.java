package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KahnTest {
    @Test
    void ordersFileWithMultipleSourcesParallelEdgesAndIsolate() throws IOException {
        Graph graph = Graph.read(Path.of("data/dag-directed.txt"));
        Metrics metrics = new Metrics();
        int[] order = Kahn.compute(graph, metrics);
        assertOrder(graph, order);
        assertTrue(Kahn.isValidOrder(graph, order));
        assertEquals(7, metrics.edgeScans);
    }

    @Test
    void handlesEmptyGraphAndIsolates() {
        assertArrayEquals(new int[0], Kahn.compute(new Graph(0, true)));
        assertArrayEquals(new int[]{0}, Kahn.compute(new Graph(1, true)));
        Graph graph = new Graph(5, true);
        assertOrder(graph, Kahn.compute(graph));
        assertTrue(Kahn.isValidOrder(graph, new int[]{4, 3, 2, 1, 0}));
    }

    @Test
    void rejectsCyclicFile() throws IOException {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        assertCycle(graph);
    }

    @Test
    void rejectsSelfLoop() {
        Graph graph = new Graph(1, true);
        graph.addEdge(0, 0, -10);
        assertCycle(graph);
        assertFalse(Kahn.isValidOrder(graph, new int[]{0}));
    }

    @Test
    void throwsInsteadOfReturningPartialOrderWhenCycleIsDisconnected() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);
        graph.addEdge(3, 4, 1);
        Metrics metrics = new Metrics();
        assertThrows(IllegalArgumentException.class, () -> Kahn.compute(graph, metrics));
        // Only 0 -> 1 is scanned; the cyclic component never enters the queue.
        assertEquals(1, metrics.edgeScans);
    }

    @Test
    void countsParallelEdgesIndividually() {
        Graph graph = new Graph(3, true);
        graph.addEdge(2, 0, Integer.MIN_VALUE);
        graph.addEdge(2, 0, Integer.MAX_VALUE);
        graph.addEdge(0, 1, -1);
        Metrics metrics = new Metrics();
        assertArrayEquals(new int[]{2, 0, 1}, Kahn.compute(graph, metrics));
        assertEquals(3, metrics.edgeScans);
    }

    @Test
    void validatesOneHundredRandomDagsWithShuffledVertexNumbers() {
        Random random = new Random(271828);
        for (int trial = 0; trial < 100; trial++) {
            int n = random.nextInt(201);
            int[] permutation = new int[n];
            for (int vertex = 0; vertex < n; vertex++) {
                permutation[vertex] = vertex;
            }
            for (int i = n - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int temporary = permutation[i];
                permutation[i] = permutation[other];
                permutation[other] = temporary;
            }
            Graph graph = new Graph(n, true);
            int m = n < 2 ? 0 : random.nextInt(6 * n + 1);
            for (int edge = 0; edge < m; edge++) {
                int a = random.nextInt(n);
                int b;
                do {
                    b = random.nextInt(n);
                } while (a == b);
                graph.addEdge(permutation[Math.min(a, b)], permutation[Math.max(a, b)],
                        random.nextInt(21) - 10);
            }
            Metrics metrics = new Metrics();
            int[] order = Kahn.compute(graph, metrics);
            assertOrder(graph, order);
            assertTrue(Kahn.isValidOrder(graph, order));
            assertEquals(m, metrics.edgeScans);
            assertEquals(0, metrics.maxDepth);
            assertEquals(0, metrics.comparisons);
            assertEquals(0, metrics.dsuOps);
        }
    }

    @Test
    void validatesPermutationAsWellAsEdgeDirections() {
        Graph graph = new Graph(3, true);
        graph.addEdge(2, 0, 1);
        assertTrue(Kahn.isValidOrder(graph, new int[]{2, 0, 1}));
        assertTrue(Kahn.isValidOrder(graph, new int[]{1, 2, 0}));
        assertFalse(Kahn.isValidOrder(graph, null));
        assertFalse(Kahn.isValidOrder(graph, new int[]{2, 0}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{2, 0, 1, 1}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{2, 0, 0}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{2, 0, -1}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{2, 0, 3}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{0, 1, 2}));
        assertTrue(Kahn.isValidOrder(new Graph(0, true), new int[0]));
    }

    @Test
    void resetsMetricsAndDoesNotModifyGraphBetweenRuns() throws IOException {
        Graph graph = Graph.read(Path.of("data/dag-directed.txt"));
        Edge first = graph.edges().get(0);
        Metrics metrics = new Metrics();
        int[] firstOrder = Kahn.compute(graph, metrics);
        metrics.dsuOps = metrics.comparisons = 100;
        metrics.maxDepth = 100;
        assertArrayEquals(firstOrder, Kahn.compute(graph, metrics));
        assertEquals(7, metrics.edgeScans);
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.comparisons);
        assertEquals(0, metrics.maxDepth);
        assertSame(first, graph.edges().get(0));
        assertEquals(7, graph.edgeCount());
        Kahn.compute(new Graph(0, true), metrics);
        assertEquals(0, metrics.edgeScans);
        assertEquals(0, metrics.maxDepth);
    }

    @Test
    void rejectsUndirectedGraphs() throws IOException {
        Graph input = Graph.read(Path.of("data/connected-undirected.txt"));
        for (Graph graph : new Graph[]{input, new Graph(0, false)}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> Kahn.compute(graph));
            assertTrue(error.getMessage().contains("directed"));
            assertThrows(IllegalArgumentException.class,
                    () -> Kahn.isValidOrder(graph, new int[graph.vertexCount()]));
        }
    }

    private static void assertCycle(Graph graph) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Kahn.compute(graph));
        assertTrue(error.getMessage().contains("cycle"));
    }

    private static void assertOrder(Graph graph, int[] order) {
        int n = graph.vertexCount();
        assertEquals(n, order.length);
        boolean[] seen = new boolean[n];
        int[] position = new int[n];
        for (int i = 0; i < n; i++) {
            int vertex = order[i];
            assertTrue(vertex >= 0 && vertex < n);
            assertFalse(seen[vertex]);
            seen[vertex] = true;
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) {
            assertTrue(position[edge.from] < position[edge.to]);
        }
    }
}
