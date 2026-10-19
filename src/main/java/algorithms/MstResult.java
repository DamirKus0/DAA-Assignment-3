package algorithms;

import graph.Edge;

import java.util.List;

public final class MstResult {
    private final List<Edge> edges;
    private final long totalWeight;
    private final int components;

    MstResult(List<Edge> edges, long totalWeight, int components) {
        this.edges = List.copyOf(edges);
        this.totalWeight = totalWeight;
        this.components = components;
    }

    public List<Edge> edges() {
        return edges;
    }

    public long totalWeight() {
        return totalWeight;
    }

    public int components() {
        return components;
    }
}
