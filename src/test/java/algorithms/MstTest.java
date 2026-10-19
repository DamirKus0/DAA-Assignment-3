package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void findsKnownTreeFromFile() throws IOException {
        Graph graph = Graph.read(Path.of("data/connected-undirected.txt"));
        assertBoth(graph, 10, 1);
    }

    @Test
    void findsKnownForestWithParallelEdgesLoopsAndIsolates() throws IOException {
        Graph graph = Graph.read(Path.of("data/disconnected-undirected.txt"));
        assertBoth(graph, 8, 4);
    }

    @Test
    void handlesEmptyGraphAndIsolatedVertices() {
        assertBoth(new Graph(0, false), 0, 0);
        assertBoth(new Graph(1, false), 0, 1);
        assertBoth(new Graph(5, false), 0, 5);
    }

    @Test
    void ignoresEvenNegativeSelfLoops() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 0, Integer.MIN_VALUE);
        graph.addEdge(1, 1, -20);
        graph.addEdge(2, 2, 3);
        assertBoth(graph, 0, 3);
        Graph singleton = new Graph(1, false);
        singleton.addEdge(0, 0, -1);
        assertBoth(singleton, 0, 1);
    }

    @Test
    void choosesCheapestParallelEdges() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 10);
        graph.addEdge(1, 0, -5);
        graph.addEdge(0, 1, -5);
        graph.addEdge(1, 2, 0);
        graph.addEdge(0, 2, 20);
        assertBoth(graph, -5, 1);
    }

    @Test
    void acceptsEqualWeightsWithoutRequiringIdenticalTrees() {
        Graph graph = new Graph(5, false);
        for (int u = 0; u < 5; u++) {
            for (int v = u + 1; v < 5; v++) {
                graph.addEdge(u, v, 7);
            }
        }
        assertBoth(graph, 28, 1);
    }

    @Test
    void comparesExtremeWeightsWithoutIntSubtractionOverflow() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(0, 2, Integer.MIN_VALUE);
        graph.addEdge(1, 2, 0);
        assertBoth(graph, Integer.MIN_VALUE, 1);
    }

    @Test
    void accumulatesPositiveAndNegativeWeightsInLong() {
        Graph positive = new Graph(4, false);
        Graph negative = new Graph(4, false);
        for (int vertex = 1; vertex < 4; vertex++) {
            positive.addEdge(vertex - 1, vertex, Integer.MAX_VALUE);
            negative.addEdge(vertex - 1, vertex, Integer.MIN_VALUE);
        }
        assertBoth(positive, 3L * Integer.MAX_VALUE, 1);
        assertBoth(negative, 3L * Integer.MIN_VALUE, 1);
    }

    @Test
    void rejectsDirectedGraphsWithClearMessages() throws IOException {
        Graph cyclic = Graph.read(Path.of("data/cyclic-directed.txt"));
        for (Graph graph : new Graph[]{cyclic, new Graph(0, true)}) {
            IllegalArgumentException kruskal = assertThrows(IllegalArgumentException.class,
                    () -> Kruskal.compute(graph));
            IllegalArgumentException prim = assertThrows(IllegalArgumentException.class,
                    () -> Prim.compute(graph));
            assertTrue(kruskal.getMessage().contains("undirected"));
            assertTrue(prim.getMessage().contains("undirected"));
        }
    }

    @Test
    void agreesOnOneHundredRandomConnectedGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(79);
            Graph graph = new Graph(n, false);
            for (int vertex = 1; vertex < n; vertex++) {
                graph.addEdge(vertex, random.nextInt(vertex), random.nextInt(2001) - 1000);
            }
            int extraEdges = random.nextInt(4 * n + 1);
            for (int edge = 0; edge < extraEdges; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(2001) - 1000);
            }
            MstResult kruskal = Kruskal.compute(graph);
            MstResult prim = Prim.compute(graph);
            assertEquals(kruskal.totalWeight(), prim.totalWeight(), "Trial " + trial);
            assertEquals(n - 1, kruskal.edges().size());
            assertEquals(n - 1, prim.edges().size());
            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());
            assertForest(graph, kruskal);
            assertForest(graph, prim);
        }
    }

    @Test
    void agreesOnRandomDisconnectedGraphs() {
        Random random = new Random(2026);
        for (int trial = 0; trial < 50; trial++) {
            int n = 4 + random.nextInt(57);
            int split = n / 2;
            Graph graph = new Graph(n, false);
            for (int edge = 0; edge < 3 * n; edge++) {
                int start = random.nextBoolean() ? 0 : split;
                int size = start == 0 ? split : n - split;
                graph.addEdge(start + random.nextInt(size), start + random.nextInt(size),
                        random.nextInt(201) - 100);
            }
            MstResult kruskal = Kruskal.compute(graph);
            MstResult prim = Prim.compute(graph);
            assertTrue(kruskal.components() >= 2);
            assertEquals(kruskal.totalWeight(), prim.totalWeight(), "Trial " + trial);
            assertForest(graph, kruskal);
            assertForest(graph, prim);
        }
    }

    @Test
    void matchesExhaustiveSearchOnSmallMultigraphs() {
        Random random = new Random(99);
        for (int trial = 0; trial < 60; trial++) {
            int n = 1 + random.nextInt(6);
            int m = random.nextInt(11);
            Graph graph = new Graph(n, false);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            long expectedWeight = exhaustiveForestWeight(graph);
            assertBoth(graph, expectedWeight, componentCount(componentLabels(graph)));
        }
    }

    @Test
    void primStartsAtZeroAndRestartsAfterAnIsolatedVertex() {
        Graph connected = new Graph(3, false);
        connected.addEdge(1, 2, -10);
        connected.addEdge(0, 1, 5);
        MstResult first = Prim.compute(connected);
        assertEquals(0, first.edges().get(0).from);
        assertEquals(1, first.edges().get(0).to);
        Graph isolatedStart = new Graph(4, false);
        isolatedStart.addEdge(1, 2, 3);
        MstResult second = Prim.compute(isolatedStart);
        assertEquals(3, second.components());
        assertEquals(1, second.edges().get(0).from);
        assertForest(isolatedStart, second);
    }

    @Test
    void countsActualScansComparisonsAndDsuOperations() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(0, 2, 100);
        Metrics metrics = new Metrics();
        Kruskal.compute(graph, metrics);
        assertEquals(2, metrics.edgeScans);
        // Each accepted edge: two explicit finds, two finds inside union, one union.
        assertEquals(10, metrics.dsuOps);
        assertTrue(metrics.comparisons > 0);
        assertEquals(0, metrics.maxDepth);
        Prim.compute(graph, metrics);
        assertEquals(6, metrics.edgeScans);
        assertEquals(0, metrics.dsuOps);
        assertTrue(metrics.comparisons > 0);
        assertEquals(0, metrics.maxDepth);

        Graph disconnected = new Graph(4, false);
        disconnected.addEdge(0, 0, -1);
        disconnected.addEdge(0, 1, 2);
        disconnected.addEdge(1, 0, 3);
        Kruskal.compute(disconnected, metrics);
        assertEquals(3, metrics.edgeScans);
        assertEquals(9, metrics.dsuOps);
        Prim.compute(disconnected, metrics);
        assertEquals(6, metrics.edgeScans);
    }

    @Test
    void resetsAllMetricsBetweenRunsAndLeavesGraphUnchanged() throws IOException {
        Graph graph = Graph.read(Path.of("data/connected-undirected.txt"));
        Edge firstInputEdge = graph.edges().get(0);
        Metrics metrics = new Metrics();
        Kruskal.compute(graph, metrics);
        long scans = metrics.edgeScans;
        long comparisons = metrics.comparisons;
        long operations = metrics.dsuOps;
        metrics.maxDepth = 123;
        Kruskal.compute(graph, metrics);
        assertEquals(scans, metrics.edgeScans);
        assertEquals(comparisons, metrics.comparisons);
        assertEquals(operations, metrics.dsuOps);
        assertEquals(0, metrics.maxDepth);
        Prim.compute(graph, metrics);
        scans = metrics.edgeScans;
        comparisons = metrics.comparisons;
        Prim.compute(graph, metrics);
        assertEquals(scans, metrics.edgeScans);
        assertEquals(comparisons, metrics.comparisons);
        assertEquals(0, metrics.dsuOps);
        assertSame(firstInputEdge, graph.edges().get(0));
        assertEquals(7, graph.edgeCount());
        assertEquals(4, graph.edges().get(0).weight);

        metrics.maxDepth = 123;
        Prim.compute(new Graph(0, false), metrics);
        assertEmptyMetrics(metrics);
        metrics.dsuOps = metrics.edgeScans = metrics.comparisons = 10;
        metrics.maxDepth = 10;
        Kruskal.compute(new Graph(0, false), metrics);
        assertEmptyMetrics(metrics);
    }

    @Test
    void returnsReadOnlyForestEdges() {
        Graph graph = new Graph(2, false);
        graph.addEdge(0, 1, 3);
        assertThrows(UnsupportedOperationException.class, () -> Kruskal.compute(graph).edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> Prim.compute(graph).edges().clear());
    }

    private static void assertBoth(Graph graph, long weight, int components) {
        for (MstResult result : new MstResult[]{Kruskal.compute(graph), Prim.compute(graph)}) {
            assertEquals(weight, result.totalWeight());
            assertEquals(components, result.components());
            assertForest(graph, result);
        }
    }

    private static void assertForest(Graph original, MstResult result) {
        Graph forest = new Graph(original.vertexCount(), false);
        long totalWeight = 0;
        for (Edge chosen : result.edges()) {
            boolean found = false;
            for (Edge input : original.edges()) {
                boolean sameEnds = input.from == chosen.from && input.to == chosen.to;
                boolean reversedEnds = input.from == chosen.to && input.to == chosen.from;
                if ((sameEnds || reversedEnds) && input.weight == chosen.weight) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Forest edge must belong to the input graph");
            forest.addEdge(chosen.from, chosen.to, chosen.weight);
            totalWeight += chosen.weight;
        }
        int[] originalLabels = componentLabels(original);
        assertArrayEquals(originalLabels, componentLabels(forest), "Forest must span every component");
        assertEquals(componentCount(originalLabels), result.components());
        // Spanning every component with n-c edges also rules out cycles and self-loops.
        assertEquals(original.vertexCount() - result.components(), result.edges().size());
        assertEquals(totalWeight, result.totalWeight());
    }

    /** BFS labels are canonical: each component gets its smallest vertex number. */
    private static int[] componentLabels(Graph graph) {
        int[] labels = new int[graph.vertexCount()];
        Arrays.fill(labels, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int start = 0; start < labels.length; start++) {
            if (labels[start] != -1) {
                continue;
            }
            labels[start] = start;
            queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Edge edge : graph.neighbors(vertex)) {
                    if (labels[edge.to] == -1) {
                        labels[edge.to] = start;
                        queue.add(edge.to);
                    }
                }
            }
        }
        return labels;
    }

    private static int componentCount(int[] labels) {
        int count = 0;
        for (int vertex = 0; vertex < labels.length; vertex++) {
            if (labels[vertex] == vertex) {
                count++;
            }
        }
        return count;
    }

    private static long exhaustiveForestWeight(Graph graph) {
        int[] expectedLabels = componentLabels(graph);
        int requiredEdges = graph.vertexCount() - componentCount(expectedLabels);
        long bestWeight = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << graph.edgeCount()); mask++) {
            if (Integer.bitCount(mask) != requiredEdges) {
                continue;
            }
            Graph candidate = new Graph(graph.vertexCount(), false);
            long weight = 0;
            for (int i = 0; i < graph.edgeCount(); i++) {
                if ((mask & (1 << i)) != 0) {
                    Edge edge = graph.edges().get(i);
                    candidate.addEdge(edge.from, edge.to, edge.weight);
                    weight += edge.weight;
                }
            }
            if (Arrays.equals(expectedLabels, componentLabels(candidate))) {
                bestWeight = Math.min(bestWeight, weight);
            }
        }
        assertNotEquals(Long.MAX_VALUE, bestWeight, "A spanning forest must exist");
        return bestWeight;
    }

    private static void assertEmptyMetrics(Metrics metrics) {
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.edgeScans);
        assertEquals(0, metrics.comparisons);
        assertEquals(0, metrics.maxDepth);
    }
}
