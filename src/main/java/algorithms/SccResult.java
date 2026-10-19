package algorithms;

import java.util.Arrays;

public final class SccResult {
    private final int[] component;
    private final int componentCount;

    SccResult(int[] component, int componentCount) {
        this.component = component.clone();
        this.componentCount = componentCount;
    }

    public int vertexCount() {
        return component.length;
    }

    public int componentCount() {
        return componentCount;
    }

    public int componentOf(int vertex) {
        if (vertex < 0 || vertex >= component.length) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
        return component[vertex];
    }

    /** IDs are in 0..componentCount-1; their numeric order has no API meaning. */
    public int[] componentIds() {
        return component.clone();
    }

    /** Linear-time comparison of partitions, allowing different component labels. */
    public boolean samePartitionAs(SccResult other) {
        if (other == null || vertexCount() != other.vertexCount() || componentCount != other.componentCount) {
            return false;
        }
        int[] forward = new int[componentCount];
        int[] backward = new int[componentCount];
        Arrays.fill(forward, -1);
        Arrays.fill(backward, -1);
        for (int vertex = 0; vertex < component.length; vertex++) {
            int from = component[vertex];
            int to = other.component[vertex];
            if (forward[from] == -1 && backward[to] == -1) {
                forward[from] = to;
                backward[to] = from;
            } else if (forward[from] != to || backward[to] != from) {
                return false;
            }
        }
        return true;
    }
}
