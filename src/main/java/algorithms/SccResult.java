package algorithms;

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
}
