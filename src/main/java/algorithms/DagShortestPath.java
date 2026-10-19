package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.Arrays;

public final class DagShortestPath {
    public static final long INFINITY = Long.MAX_VALUE;

    private DagShortestPath() {
    }

    public static PathResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static PathResult compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("DAG shortest paths require a directed graph");
        }
        metrics.reset();
        // Kahn uses its own counters; this run counts only the relaxation pass.
        int[] order = Kahn.compute(graph);
        int n = graph.vertexCount();
        int[] inDegree = new int[n];
        for (Edge edge : graph.edges()) {
            inDegree[edge.to]++;
        }
        long[] distance = new long[n];
        Arrays.fill(distance, INFINITY);
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        Edge[] parentEdge = new Edge[n];
        for (int vertex = 0; vertex < n; vertex++) {
            if (inDegree[vertex] == 0) {
                distance[vertex] = 0;
            }
        }

        for (int vertex : order) {
            if (distance[vertex] == INFINITY) {
                continue;
            }
            for (Edge edge : graph.neighbors(vertex)) {
                metrics.edgeScans++;
                long candidate = distance[vertex] + edge.weight;
                metrics.comparisons++;
                if (candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    parent[edge.to] = vertex;
                    // Keep the exact edge, since parallel edges may have different weights.
                    parentEdge[edge.to] = edge;
                }
            }
        }
        return new PathResult(distance, parent, parentEdge);
    }
}
