package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Graph {
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges;

    public Graph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Vertex count must be non-negative");
        }
        this.directed = directed;
        adjacency = new ArrayList<>(vertexCount);
        edges = new ArrayList<>();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertexCount() {
        return adjacency.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed) {
            // Two incidences even for a self-loop: undirected adjacency has 2m entries.
            adjacency.get(to).add(new Edge(to, from, weight));
        }
    }

    /** Read-only view: each input edge occurs once, including undirected edges. */
    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> neighbors(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
    }

    public static Graph read(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String[] header = fields(reader.readLine(), 3, 1);
            int vertexCount = integer(header[0], 1);
            int edgeCount = integer(header[1], 1);
            if (vertexCount < 0 || edgeCount < 0) {
                throw new IOException("Line 1: n and m must be non-negative");
            }
            if (!header[2].equals("directed") && !header[2].equals("undirected")) {
                throw new IOException("Line 1: expected directed or undirected");
            }
            Graph graph = new Graph(vertexCount, header[2].equals("directed"));
            for (int i = 0; i < edgeCount; i++) {
                int lineNumber = i + 2;
                String[] edge = fields(reader.readLine(), 3, lineNumber);
                int from = integer(edge[0], lineNumber);
                int to = integer(edge[1], lineNumber);
                int weight = integer(edge[2], lineNumber);
                try {
                    graph.addEdge(from, to, weight);
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Line " + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    throw new IOException("Unexpected data after " + edgeCount + " edges");
                }
            }
            return graph;
        }
    }

    private static String[] fields(String line, int count, int lineNumber) throws IOException {
        if (line == null || line.isBlank()) {
            throw new IOException("Line " + lineNumber + ": expected " + count + " fields");
        }
        String[] fields = line.trim().split("\\s+");
        if (fields.length != count) {
            throw new IOException("Line " + lineNumber + ": expected " + count + " fields");
        }
        return fields;
    }

    private static int integer(String value, int lineNumber) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Line " + lineNumber + ": invalid int: " + value, exception);
        }
    }
}
