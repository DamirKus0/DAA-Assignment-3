package algorithms;

import graph.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PathResult {
    private final long[] distance;
    private final int[] parent;
    private final Edge[] parentEdge;

    PathResult(long[] distance, int[] parent, Edge[] parentEdge) {
        this.distance = distance.clone();
        this.parent = parent.clone();
        this.parentEdge = parentEdge.clone();
    }

    public int vertexCount() {
        return distance.length;
    }

    public long distanceTo(int vertex) {
        checkVertex(vertex);
        return distance[vertex];
    }

    public int parentOf(int vertex) {
        checkVertex(vertex);
        return parent[vertex];
    }

    public long[] distances() {
        return distance.clone();
    }

    public int[] parents() {
        return parent.clone();
    }

    /** Returns vertices from a source to the target, including both endpoints. */
    public List<Integer> pathTo(int vertex) {
        checkVertex(vertex);
        if (distance[vertex] == DagShortestPath.INFINITY) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        for (int current = vertex; current != -1; current = parent[current]) {
            path.add(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    /** Returns the actual chosen edges, resolving ambiguity between parallel edges. */
    public List<Edge> edgePathTo(int vertex) {
        checkVertex(vertex);
        List<Edge> path = new ArrayList<>();
        for (int current = vertex; parent[current] != -1; current = parent[current]) {
            path.add(parentEdge[current]);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    public long sumDistances() {
        long sum = 0;
        for (long value : distance) {
            if (value == DagShortestPath.INFINITY) {
                throw new IllegalStateException("Cannot sum an infinite distance");
            }
            // Individual distances fit long; their aggregate may exceed its range.
            sum = Math.addExact(sum, value);
        }
        return sum;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= distance.length) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
    }
}
