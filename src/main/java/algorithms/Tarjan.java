package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.Arrays;
import java.util.List;

public final class Tarjan {
    private Tarjan() {
    }

    public static SccResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static SccResult compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Tarjan requires a directed graph");
        }
        metrics.reset();
        int n = graph.vertexCount();
        int[] index = new int[n];
        Arrays.fill(index, -1);
        int[] lowlink = new int[n];
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int[] nextEdge = new int[n];
        int[] dfsStack = new int[n];
        int[] activeStack = new int[n];
        boolean[] onStack = new boolean[n];
        int activeCount = 0;
        int nextIndex = 0;
        int componentCount = 0;

        for (int start = 0; start < n; start++) {
            if (index[start] != -1) {
                continue;
            }
            int depth = 1;
            dfsStack[0] = start;
            index[start] = lowlink[start] = nextIndex++;
            activeStack[activeCount++] = start;
            onStack[start] = true;
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            while (depth > 0) {
                int vertex = dfsStack[depth - 1];
                List<Edge> edges = graph.neighbors(vertex);
                if (nextEdge[vertex] < edges.size()) {
                    Edge edge = edges.get(nextEdge[vertex]++);
                    metrics.edgeScans++;
                    int neighbor = edge.to;
                    if (index[neighbor] == -1) {
                        index[neighbor] = lowlink[neighbor] = nextIndex++;
                        dfsStack[depth++] = neighbor;
                        activeStack[activeCount++] = neighbor;
                        onStack[neighbor] = true;
                        metrics.maxDepth = Math.max(metrics.maxDepth, depth);
                    } else if (onStack[neighbor]) {
                        lowlink[vertex] = Math.min(lowlink[vertex], index[neighbor]);
                    }
                } else {
                    depth--;
                    if (depth > 0) {
                        int parent = dfsStack[depth - 1];
                        lowlink[parent] = Math.min(lowlink[parent], lowlink[vertex]);
                    }
                    if (lowlink[vertex] == index[vertex]) {
                        int member;
                        do {
                            member = activeStack[--activeCount];
                            onStack[member] = false;
                            component[member] = componentCount;
                        } while (member != vertex);
                        componentCount++;
                    }
                }
            }
        }
        return new SccResult(component, componentCount);
    }
}
