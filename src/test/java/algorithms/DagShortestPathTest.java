package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DagShortestPathTest {
    @Test
    void findsKnownDistancesAndPathsFromFile() throws IOException {
        Graph graph = Graph.read(Path.of("data/dag-directed.txt"));
        PathResult result = DagShortestPath.compute(graph);
        assertArrayEquals(new long[]{-2, -2, 1, 0, 0, 0, 0}, result.distances());
        assertEquals(-3, result.sumDistances());
        assertEquals(List.of(4, 2, 0, 3), result.pathTo(3));
        assertEquals(List.of(4, 1), result.pathTo(1));
        assertSame(graph.edges().get(1), result.edgePathTo(1).get(0));
        assertPaths(graph, result);
    }

    @Test
    void handlesEmptyGraphAndIsolatedVertices() {
        PathResult empty = DagShortestPath.compute(new Graph(0, true));
        assertEquals(0, empty.vertexCount());
        assertArrayEquals(new long[0], empty.distances());
        assertArrayEquals(new int[0], empty.parents());
        assertEquals(0, empty.sumDistances());
        for (int n : new int[]{1, 5}) {
            Graph graph = new Graph(n, true);
            PathResult result = DagShortestPath.compute(graph);
            assertArrayEquals(new long[n], result.distances());
            for (int vertex = 0; vertex < n; vertex++) {
                assertEquals(-1, result.parentOf(vertex));
                assertEquals(List.of(vertex), result.pathTo(vertex));
                assertTrue(result.edgePathTo(vertex).isEmpty());
            }
            assertPaths(graph, result);
        }
    }

    @Test
    void initializesOnlySourcesToZero() {
        Graph graph = new Graph(3, true);
        graph.addEdge(2, 1, 7);
        graph.addEdge(1, 0, 4);
        PathResult result = DagShortestPath.compute(graph);
        assertArrayEquals(new long[]{11, 7, 0}, result.distances());
        assertEquals(List.of(2, 1, 0), result.pathTo(0));
        assertPaths(graph, result);
    }

    @Test
    void usesEverySourceAcrossDisconnectedComponents() {
        Graph graph = new Graph(8, true);
        graph.addEdge(2, 0, 9);
        graph.addEdge(3, 0, 2);
        graph.addEdge(0, 1, -6);
        graph.addEdge(5, 4, 7);
        graph.addEdge(6, 4, -3);
        PathResult result = DagShortestPath.compute(graph);
        assertArrayEquals(new long[]{2, -4, 0, 0, -3, 0, 0, 0}, result.distances());
        assertEquals(List.of(3, 0, 1), result.pathTo(1));
        assertEquals(List.of(6, 4), result.pathTo(4));
        assertPaths(graph, result);
    }

    @Test
    void negativeEdgeCanImproveAnEarlierCandidate() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 2, 10);
        graph.addEdge(2, 1, -20);
        graph.addEdge(1, 3, 3);
        PathResult result = DagShortestPath.compute(graph);
        assertArrayEquals(new long[]{0, -10, 10, -7}, result.distances());
        assertEquals(List.of(0, 2, 1, 3), result.pathTo(3));
        assertPaths(graph, result);
    }

    @Test
    void recordsTheActualCheapestParallelEdge() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 9);
        graph.addEdge(0, 1, -4);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 5);
        PathResult result = DagShortestPath.compute(graph);
        assertEquals(1, result.distanceTo(2));
        assertSame(graph.edges().get(1), result.edgePathTo(2).get(0));
        assertEquals(-4, result.edgePathTo(2).get(0).weight);
        assertPaths(graph, result);
    }

    @Test
    void equalAndZeroWeightPathsStillRestoreAValidSourcePath() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 2, 0);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 3, 0);
        graph.addEdge(0, 3, 0);
        PathResult result = DagShortestPath.compute(graph);
        assertArrayEquals(new long[4], result.distances());
        assertPaths(graph, result);
    }

    @Test
    void accumulatesDistancesAndTheirSumInLong() {
        for (int weight : new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            Graph graph = new Graph(4, true);
            // Reverse insertion order makes the reference need multiple passes.
            for (int vertex = 3; vertex > 0; vertex--) {
                graph.addEdge(vertex - 1, vertex, weight);
            }
            PathResult result = DagShortestPath.compute(graph);
            assertArrayEquals(new long[]{0, weight, 2L * weight, 3L * weight}, result.distances());
            assertEquals(6L * weight, result.sumDistances());
            assertArrayEquals(bellmanFord(graph), result.distances());
            assertPaths(graph, result);
        }
    }

    @Test
    void matchesBellmanFordOnOneHundredRandomDagsWithNegativeWeights() {
        Random random = new Random(424242);
        for (int trial = 0; trial < 100; trial++) {
            int n = random.nextInt(41);
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
            int m = n < 2 ? 0 : random.nextInt(5 * n + 1);
            for (int edge = 0; edge < m; edge++) {
                int a = random.nextInt(n);
                int b;
                do {
                    b = random.nextInt(n);
                } while (a == b);
                graph.addEdge(permutation[Math.min(a, b)], permutation[Math.max(a, b)],
                        random.nextInt(201) - 100);
            }
            Metrics metrics = new Metrics();
            PathResult result = DagShortestPath.compute(graph, metrics);
            long[] expected = bellmanFord(graph);
            assertArrayEquals(expected, result.distances(), "Trial " + trial);
            long sum = 0;
            for (long distance : expected) {
                sum += distance;
            }
            assertEquals(sum, result.sumDistances());
            assertEquals(m, metrics.edgeScans);
            assertEquals(m, metrics.comparisons);
            assertEquals(0, metrics.dsuOps);
            assertEquals(0, metrics.maxDepth);
            assertPaths(graph, result);
        }
    }

    @Test
    void rejectsCyclicFileInsteadOfReturningDistances() throws IOException {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        assertCycle(graph);
    }

    @Test
    void rejectsSelfLoopAndCycleInSeparateComponent() {
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, 1);
        assertCycle(loop);
        Graph disconnected = new Graph(5, true);
        disconnected.addEdge(0, 1, -3);
        disconnected.addEdge(2, 3, 2);
        disconnected.addEdge(3, 2, 1);
        assertCycle(disconnected);
    }

    @Test
    void rejectsUndirectedGraphs() throws IOException {
        Graph input = Graph.read(Path.of("data/connected-undirected.txt"));
        for (Graph graph : new Graph[]{input, new Graph(0, false)}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> DagShortestPath.compute(graph));
            assertTrue(error.getMessage().contains("directed"));
        }
    }

    @Test
    void countsOnlyRelaxationAndResetsMetricsWithoutChangingGraph() throws IOException {
        Graph graph = Graph.read(Path.of("data/dag-directed.txt"));
        Edge first = graph.edges().get(0);
        Metrics metrics = new Metrics();
        Kahn.compute(graph, metrics);
        metrics.dsuOps = 100;
        metrics.maxDepth = 100;
        metrics.comparisons = 100;
        PathResult firstResult = DagShortestPath.compute(graph, metrics);
        assertEquals(7, metrics.edgeScans);
        assertEquals(7, metrics.comparisons);
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.maxDepth);
        PathResult secondResult = DagShortestPath.compute(graph, metrics);
        assertArrayEquals(firstResult.distances(), secondResult.distances());
        assertArrayEquals(firstResult.parents(), secondResult.parents());
        assertEquals(7, metrics.edgeScans);
        assertEquals(7, metrics.comparisons);
        assertSame(first, graph.edges().get(0));
        assertEquals(7, graph.edgeCount());
        DagShortestPath.compute(new Graph(0, true), metrics);
        assertEquals(0, metrics.edgeScans);
        assertEquals(0, metrics.comparisons);
    }

    @Test
    void resultIsIndependentOfReturnedArraysListsAndLaterGraphChanges() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, 5);
        PathResult result = DagShortestPath.compute(graph);
        result.distances()[1] = -100;
        result.parents()[1] = -1;
        assertEquals(5, result.distanceTo(1));
        assertEquals(0, result.parentOf(1));
        assertThrows(UnsupportedOperationException.class, () -> result.pathTo(1).clear());
        assertThrows(UnsupportedOperationException.class, () -> result.edgePathTo(1).clear());
        graph.addEdge(0, 1, -10);
        assertEquals(5, result.distanceTo(1));
        assertEquals(5, result.edgePathTo(1).get(0).weight);
        assertEquals(-10, DagShortestPath.compute(graph).distanceTo(1));
    }

    @Test
    void checksVertexBoundsOnAllVertexAccessors() {
        PathResult result = DagShortestPath.compute(new Graph(2, true));
        for (int vertex : new int[]{-1, 2}) {
            assertThrows(IllegalArgumentException.class, () -> result.distanceTo(vertex));
            assertThrows(IllegalArgumentException.class, () -> result.parentOf(vertex));
            assertThrows(IllegalArgumentException.class, () -> result.pathTo(vertex));
            assertThrows(IllegalArgumentException.class, () -> result.edgePathTo(vertex));
        }
        PathResult empty = DagShortestPath.compute(new Graph(0, true));
        assertThrows(IllegalArgumentException.class, () -> empty.pathTo(0));
    }

    @Test
    void restoresOneHundredThousandVertexPathWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 1; vertex < n; vertex++) {
            graph.addEdge(vertex - 1, vertex, -1);
        }
        PathResult result = DagShortestPath.compute(graph);
        assertEquals(-(n - 1L), result.distanceTo(n - 1));
        assertEquals(-((long) n * (n - 1) / 2), result.sumDistances());
        List<Integer> path = result.pathTo(n - 1);
        List<Edge> edges = result.edgePathTo(n - 1);
        assertEquals(n, path.size());
        assertEquals(n - 1, edges.size());
        long weight = 0;
        for (int vertex = 0; vertex < n; vertex++) {
            assertEquals(vertex, path.get(vertex).intValue());
            assertEquals(-((long) vertex), result.distanceTo(vertex));
            if (vertex > 0) {
                Edge edge = edges.get(vertex - 1);
                assertEquals(vertex - 1, edge.from);
                assertEquals(vertex, edge.to);
                weight += edge.weight;
            }
        }
        assertEquals(result.distanceTo(n - 1), weight);
    }

    @Test
    void detectsOverflowOfAggregateEvenWhenIndividualDistancesFitLong() {
        int n = 100_000;
        for (int weight : new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            Graph graph = new Graph(n, true);
            for (int vertex = 1; vertex < n; vertex++) {
                graph.addEdge(vertex - 1, vertex, weight);
            }
            PathResult result = DagShortestPath.compute(graph);
            assertEquals((n - 1L) * weight, result.distanceTo(n - 1));
            assertThrows(ArithmeticException.class, result::sumDistances);
        }
    }

    /** Independent reference: repeated full edge passes, no topological order. */
    private static long[] bellmanFord(Graph graph) {
        int n = graph.vertexCount();
        int[] inDegree = new int[n];
        for (Edge edge : graph.edges()) {
            inDegree[edge.to]++;
        }
        long[] distance = new long[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        for (int vertex = 0; vertex < n; vertex++) {
            if (inDegree[vertex] == 0) {
                distance[vertex] = 0;
            }
        }
        for (int pass = 1; pass < n; pass++) {
            boolean changed = false;
            for (Edge edge : graph.edges()) {
                if (distance[edge.from] != Long.MAX_VALUE) {
                    long candidate = distance[edge.from] + edge.weight;
                    if (candidate < distance[edge.to]) {
                        distance[edge.to] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return distance;
    }

    private static void assertPaths(Graph graph, PathResult result) {
        int n = graph.vertexCount();
        assertEquals(n, result.vertexCount());
        int[] inDegree = new int[n];
        for (Edge edge : graph.edges()) {
            inDegree[edge.to]++;
        }
        for (int target = 0; target < n; target++) {
            assertNotEquals(Long.MAX_VALUE, result.distanceTo(target));
            List<Integer> vertices = result.pathTo(target);
            List<Edge> edges = result.edgePathTo(target);
            assertFalse(vertices.isEmpty());
            assertEquals(0, inDegree[vertices.get(0)]);
            assertEquals(target, vertices.get(vertices.size() - 1).intValue());
            assertEquals(vertices.size() - 1, edges.size());
            boolean[] seen = new boolean[n];
            long weight = 0;
            for (int i = 0; i < vertices.size(); i++) {
                int vertex = vertices.get(i);
                assertFalse(seen[vertex], "Restored path must not contain a cycle");
                seen[vertex] = true;
                if (i == 0) {
                    assertEquals(-1, result.parentOf(vertex));
                    assertEquals(0, result.distanceTo(vertex));
                } else {
                    Edge edge = edges.get(i - 1);
                    assertEquals(vertices.get(i - 1).intValue(), edge.from);
                    assertEquals(vertex, edge.to);
                    assertTrue(graph.edges().contains(edge), "Must restore an actual input edge");
                    assertEquals(edge.from, result.parentOf(vertex));
                    weight += edge.weight;
                }
            }
            assertEquals(result.distanceTo(target), weight);
        }
    }

    private static void assertCycle(Graph graph) {
        Metrics metrics = new Metrics();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> DagShortestPath.compute(graph, metrics));
        assertTrue(error.getMessage().contains("cycle"));
        assertEquals(0, metrics.edgeScans, "No relaxations before cycle validation");
    }
}
