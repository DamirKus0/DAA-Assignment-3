package algorithms;

import graph.Edge;
import graph.Graph;

import java.util.HashSet;
import java.util.Set;

/** Each original vertex is one unit of work; an SCC is an indivisible block. */
public final class Condensation {
    private final Graph graph;
    private final int[] duration;
    private final SccResult components;

    private Condensation(Graph graph, int[] duration, SccResult components) {
        this.graph = graph;
        this.duration = duration;
        this.components = components;
    }

    public static Condensation build(Graph original) {
        return build(original, Tarjan.compute(original));
    }

    /** The supplied partition must be an SCC result for this original graph. */
    public static Condensation build(Graph original, SccResult components) {
        if (!original.isDirected() || original.vertexCount() != components.vertexCount()) {
            throw new IllegalArgumentException("Condensation requires a directed graph and its SCC partition");
        }
        Graph dag = new Graph(components.componentCount(), true);
        int[] duration = new int[components.componentCount()];
        for (int vertex = 0; vertex < original.vertexCount(); vertex++) {
            duration[components.componentOf(vertex)]++;
        }
        Set<Long> seen = new HashSet<>();
        for (Edge edge : original.edges()) {
            int from = components.componentOf(edge.from);
            int to = components.componentOf(edge.to);
            if (from != to) {
                long key = ((long) from << 32) | (to & 0xffffffffL);
                if (seen.add(key)) {
                    // Dependencies have zero lag; original edge weights are not durations.
                    dag.addEdge(from, to, 0);
                }
            }
        }
        return new Condensation(dag, duration, components);
    }

    /** Graph returned for analysis; callers should not mutate the condensation. */
    public Graph graph() {
        return graph;
    }

    public int durationOf(int component) {
        if (component < 0 || component >= duration.length) {
            throw new IllegalArgumentException("Component out of range: " + component);
        }
        return duration[component];
    }

    public SccResult components() {
        return components;
    }
}
