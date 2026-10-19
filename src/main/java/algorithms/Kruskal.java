package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.List;

public final class Kruskal {
    private Kruskal() {
    }

    public static MstResult compute(Graph graph) {
        return compute(graph, new Metrics());
    }

    public static MstResult compute(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected graph");
        }
        metrics.reset();
        List<Edge> sortedEdges = new ArrayList<>(graph.edges());
        sortedEdges.sort((first, second) -> {
            metrics.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });

        DSU dsu = new DSU(graph.vertexCount());
        List<Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        for (Edge edge : sortedEdges) {
            // Once connected, all remaining edges would create a cycle.
            if (dsu.components() <= 1) {
                break;
            }
            metrics.edgeScans++;
            if (dsu.find(edge.from) != dsu.find(edge.to)) {
                dsu.union(edge.from, edge.to);
                forest.add(edge);
                totalWeight += edge.weight;
            }
        }
        metrics.dsuOps = dsu.dsuOps();
        return new MstResult(forest, totalWeight, dsu.components());
    }
}
