package graph;

/** An immutable weighted edge; in an adjacency list, from is the list's vertex. */
public final class Edge {
    public final int from;
    public final int to;
    public final int weight;

    public Edge(int from, int to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }
}
