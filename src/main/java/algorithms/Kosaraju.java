package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.Arrays;
import java.util.List;

public final class Kosaraju {
    private Kosaraju() {
    }

    public static SccResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static SccResult compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Kosaraju requires a directed graph");
        }
        metrics.reset();
        int[] order = finishOrder(graph, metrics);
        Graph reversed = reverse(graph);
        return assignComponents(reversed, order, metrics);
    }

    private static int[] finishOrder(Graph graph, Metrics metrics) {
        int n = graph.vertexCount();
        boolean[] visited = new boolean[n];
        int[] nextEdge = new int[n];
        int[] stack = new int[n];
        int[] order = new int[n];
        int finished = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            visited[start] = true;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                List<Edge> edges = graph.neighbors(vertex);
                if (nextEdge[vertex] < edges.size()) {
                    Edge edge = edges.get(nextEdge[vertex]++);
                    metrics.edgeScans++;
                    if (!visited[edge.to]) {
                        visited[edge.to] = true;
                        stack[depth++] = edge.to;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                } else {
                    // Finish only after every outgoing edge and descendant is processed.
                    order[finished++] = vertex;
                    depth--;
                }
            }
        }
        return order;
    }

    private static Graph reverse(Graph graph) {
        Graph reversed = new Graph(graph.vertexCount(), true);
        // Preparation is separate from the two DFS edge-scan counters.
        for (Edge edge : graph.edges()) {
            reversed.addEdge(edge.to, edge.from, edge.weight);
        }
        return reversed;
    }

    private static SccResult assignComponents(Graph graph, int[] order, Metrics metrics) {
        int n = graph.vertexCount();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] nextEdge = new int[n];
        int[] stack = new int[n];
        int componentCount = 0;
        for (int i = n - 1; i >= 0; i--) {
            int start = order[i];
            if (component[start] != -1) {
                continue;
            }
            int depth = 1;
            stack[0] = start;
            component[start] = componentCount;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                List<Edge> edges = graph.neighbors(vertex);
                if (nextEdge[vertex] < edges.size()) {
                    Edge edge = edges.get(nextEdge[vertex]++);
                    metrics.edgeScans++;
                    if (component[edge.to] == -1) {
                        component[edge.to] = componentCount;
                        stack[depth++] = edge.to;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    }
                } else {
                    depth--;
                }
            }
            componentCount++;
        }
        return new SccResult(component, componentCount);
    }
}
