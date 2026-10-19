package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CriticalPathTest {
    @Test
    void condensesCyclesAndDeduplicatesDependenciesWithExplicitDurations() {
        Graph graph = new Graph(7, true);
        graph.addEdge(0, 1, -5);
        graph.addEdge(1, 2, 10);
        graph.addEdge(2, 0, 8);
        graph.addEdge(2, 3, 999);
        graph.addEdge(2, 3, -999);
        graph.addEdge(3, 4, -7);
        graph.addEdge(4, 3, 3);
        graph.addEdge(4, 5, 6);
        graph.addEdge(0, 5, 1);
        Condensation result = Condensation.build(graph);
        assertEquals(4, result.graph().vertexCount());
        assertEquals(3, result.graph().edgeCount());
        assertEquals(3, result.durationOf(result.components().componentOf(0)));
        assertEquals(2, result.durationOf(result.components().componentOf(3)));
        CriticalPath.Result path = CriticalPath.compute(result);
        assertEquals(6, path.duration());
        assertEquals(3, path.componentPath().size());
        assertCondensation(graph, result);
        assertPath(result, path);
    }

    @Test
    void handlesEmptyIsolatedAndOneStrongComponent() {
        Condensation empty = Condensation.build(new Graph(0, true));
        assertEquals(0, CriticalPath.compute(empty).duration());
        assertTrue(CriticalPath.compute(empty).componentPath().isEmpty());
        assertEquals(1, CriticalPath.compute(Condensation.build(new Graph(5, true))).duration());
        Graph graph = new Graph(3, true);
        for (int vertex = 0; vertex < 3; vertex++) {
            graph.addEdge(vertex, (vertex + 1) % 3, -100);
            graph.addEdge(vertex, vertex, 100);
        }
        Condensation condensation = Condensation.build(graph);
        assertEquals(0, condensation.graph().edgeCount());
        assertEquals(3, CriticalPath.compute(condensation).duration());
        assertCondensation(graph, condensation);
    }

    @Test
    void matchesExhaustivePathEnumerationOnRandomCondensations() {
        Random random = new Random(123456);
        for (int trial = 0; trial < 100; trial++) {
            int n = random.nextInt(9);
            Graph graph = new Graph(n, true);
            int m = n == 0 ? 0 : random.nextInt(3 * n + 1);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(41) - 20);
            }
            Condensation condensation = Condensation.build(graph);
            Metrics metrics = new Metrics();
            CriticalPath.Result result = CriticalPath.compute(condensation, metrics);
            assertEquals(enumeratePaths(condensation), result.duration());
            assertEquals(condensation.graph().edgeCount(), metrics.edgeScans);
            assertEquals(condensation.graph().edgeCount() + Math.max(0, condensation.graph().vertexCount() - 1),
                    metrics.comparisons);
            assertCondensation(graph, condensation);
            assertPath(condensation, result);
        }
    }

    @Test
    void handlesOneHundredThousandVertexChainWithoutRecursion() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 1; vertex < n; vertex++) {
            graph.addEdge(vertex - 1, vertex, -100);
        }
        Condensation condensation = Condensation.build(graph);
        CriticalPath.Result result = CriticalPath.compute(condensation);
        assertEquals(n, result.duration());
        assertEquals(n, result.componentPath().size());
        // Original weights do not affect the unit-task duration model.
    }

    @Test
    void resetsMetricsProtectsResultsAndRejectsBadInputs() {
        Condensation condensation = Condensation.build(new Graph(2, true));
        Metrics metrics = new Metrics();
        metrics.dsuOps = metrics.comparisons = metrics.edgeScans = 99;
        metrics.maxDepth = 99;
        CriticalPath.Result result = CriticalPath.compute(condensation, metrics);
        assertEquals(0, metrics.edgeScans);
        assertEquals(1, metrics.comparisons);
        assertEquals(0, metrics.dsuOps);
        assertEquals(0, metrics.maxDepth);
        result.finishTimes()[0] = -1;
        assertEquals(1, result.finishTimes()[0]);
        assertThrows(UnsupportedOperationException.class, () -> result.componentPath().clear());
        assertThrows(IllegalArgumentException.class, () -> condensation.durationOf(-1));
        assertThrows(IllegalArgumentException.class, () -> condensation.durationOf(2));
        assertThrows(IllegalArgumentException.class, () -> Condensation.build(new Graph(2, false)));
        assertThrows(IllegalArgumentException.class,
                () -> Condensation.build(new Graph(3, true), Tarjan.compute(new Graph(2, true))));
    }

    private static void assertCondensation(Graph original, Condensation condensation) {
        Graph dag = condensation.graph();
        assertTrue(Kahn.isValidOrder(dag, Kahn.compute(dag)));
        Set<Long> expected = new HashSet<>();
        for (Edge edge : original.edges()) {
            int from = condensation.components().componentOf(edge.from);
            int to = condensation.components().componentOf(edge.to);
            if (from != to) {
                expected.add(((long) from << 32) | to);
            }
        }
        Set<Long> actual = new HashSet<>();
        for (Edge edge : dag.edges()) {
            assertNotEquals(edge.from, edge.to);
            assertEquals(0, edge.weight);
            assertTrue(actual.add(((long) edge.from << 32) | edge.to));
        }
        assertEquals(expected, actual);
        int durationSum = 0;
        for (int vertex = 0; vertex < dag.vertexCount(); vertex++) {
            durationSum += condensation.durationOf(vertex);
        }
        assertEquals(original.vertexCount(), durationSum);
    }

    private static long enumeratePaths(Condensation condensation) {
        Graph graph = condensation.graph();
        ArrayDeque<long[]> stack = new ArrayDeque<>();
        // All durations are positive, so enumerating starts at every vertex is equivalent.
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            stack.push(new long[]{vertex, condensation.durationOf(vertex)});
        }
        long best = 0;
        while (!stack.isEmpty()) {
            long[] state = stack.pop();
            best = Math.max(best, state[1]);
            for (Edge edge : graph.neighbors((int) state[0])) {
                stack.push(new long[]{edge.to, state[1] + condensation.durationOf(edge.to)});
            }
        }
        return best;
    }

    private static void assertPath(Condensation condensation, CriticalPath.Result result) {
        List<Integer> path = result.componentPath();
        long total = 0;
        for (int i = 0; i < path.size(); i++) {
            int vertex = path.get(i);
            total += condensation.durationOf(vertex);
            if (i > 0) {
                boolean found = false;
                for (Edge edge : condensation.graph().neighbors(path.get(i - 1))) {
                    if (edge.to == vertex) {
                        found = true;
                    }
                }
                assertTrue(found);
            }
        }
        assertEquals(result.duration(), total);
    }
}
