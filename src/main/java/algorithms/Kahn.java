package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class Kahn {
    private Kahn() {
    }

    public static int[] compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static int[] compute(Graph graph, Metrics metrics) {
        requireDirected(graph);
        metrics.reset();
        int n = graph.vertexCount();
        int[] inDegree = new int[n];
        // Preparation: these m edges are not part of the queue-pass counter.
        for (Edge edge : graph.edges()) {
            inDegree[edge.to]++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < n; vertex++) {
            if (inDegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        int[] order = new int[n];
        int processed = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order[processed++] = vertex;
            for (Edge edge : graph.neighbors(vertex)) {
                metrics.edgeScans++;
                inDegree[edge.to]--;
                if (inDegree[edge.to] == 0) {
                    queue.add(edge.to);
                }
            }
        }
        if (processed != n) {
            throw new IllegalArgumentException("Graph contains a directed cycle; topological order does not exist");
        }
        return order;
    }

    /** Checks the entire permutation and every edge, independently of compute(). */
    public static boolean isValidOrder(Graph graph, int[] order) {
        requireDirected(graph);
        int n = graph.vertexCount();
        if (order == null || order.length != n) {
            return false;
        }
        int[] position = new int[n];
        Arrays.fill(position, -1);
        for (int i = 0; i < n; i++) {
            int vertex = order[i];
            if (vertex < 0 || vertex >= n || position[vertex] != -1) {
                return false;
            }
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) {
            if (position[edge.from] >= position[edge.to]) {
                return false;
            }
        }
        return true;
    }

    private static void requireDirected(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Kahn requires a directed graph");
        }
    }
}
