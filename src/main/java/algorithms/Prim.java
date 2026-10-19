package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() {
    }

    public static MstResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static MstResult compute(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }
        metrics.reset();
        boolean[] visited = new boolean[graph.vertexCount()];
        PriorityQueue<Edge> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });
        List<Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        int componentCount = 0;

        // Start at 0, then restart at the first unvisited vertex of each component.
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            componentCount++;
            visited[start] = true;
            addEdges(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                if (visited[edge.to]) {
                    continue;
                }
                forest.add(edge);
                totalWeight += edge.weight;
                visited[edge.to] = true;
                addEdges(graph, edge.to, visited, queue, metrics);
            }
        }
        return new MstResult(forest, totalWeight, componentCount);
    }

    private static void addEdges(Graph graph, int vertex, boolean[] visited,
                                 PriorityQueue<Edge> queue, Metrics metrics) {
        for (Edge edge : graph.neighbors(vertex)) {
            metrics.edgeScans++;
            if (!visited[edge.to]) {
                queue.add(edge);
            }
        }
    }
}
