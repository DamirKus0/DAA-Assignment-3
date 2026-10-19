package benchmark;

import graph.Graph;

import java.util.Random;

public final class GraphGenerator {
    private GraphGenerator() {
    }

    public static Graph generate(int n, int m, String kind) {
        if (!kind.equals("undirected") && !kind.equals("directed") && !kind.equals("dag")) {
            throw new IllegalArgumentException("Unknown graph kind: " + kind);
        }
        boolean undirected = kind.equals("undirected");
        if (n < 0 || m < 0 || (n < 2 && m > 0) || (undirected && m < n - 1)) {
            throw new IllegalArgumentException("Invalid n/m for the requested graph kind");
        }
        Random random = new Random(42);
        Graph graph = new Graph(n, !undirected);
        int[] rank = new int[n];
        for (int vertex = 0; vertex < n; vertex++) {
            rank[vertex] = vertex;
        }
        if (!kind.equals("directed")) {
            for (int i = n - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int temporary = rank[i];
                rank[i] = rank[other];
                rank[other] = temporary;
            }
        }
        if (undirected) {
            for (int i = 1; i < n; i++) {
                int from = rank[random.nextInt(i)];
                graph.addEdge(from, rank[i], 1 + random.nextInt(1000));
            }
        }
        while (graph.edgeCount() < m) {
            int from = random.nextInt(n);
            int to;
            do {
                to = random.nextInt(n);
            } while (to == from);
            if (kind.equals("dag") && rank[from] > rank[to]) {
                int temporary = from;
                from = to;
                to = temporary;
            }
            graph.addEdge(from, to, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
