package graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsConnectedUndirectedExample() throws IOException {
        Graph graph = Graph.read(Path.of("data/connected-undirected.txt"));
        assertFalse(graph.isDirected());
        assertEquals(5, graph.vertexCount());
        assertEquals(7, graph.edgeCount());
        assertEquals(14, adjacencyEntries(graph));
        assertEdge(graph.neighbors(0).get(0), 0, 1, 4);
        assertEdge(graph.neighbors(1).get(0), 1, 0, 4);
        assertEquals(32, totalInputWeight(graph));
    }

    @Test
    void readsDisconnectedExampleWithParallelEdgesLoopsAndIsolates() throws IOException {
        Graph graph = Graph.read(Path.of("data/disconnected-undirected.txt"));
        assertEquals(7, graph.vertexCount());
        assertEquals(6, graph.edgeCount());
        assertEquals(12, adjacencyEntries(graph));
        assertEquals(2, graph.neighbors(0).size());
        assertEdge(graph.neighbors(0).get(0), 0, 1, 4);
        assertEdge(graph.neighbors(0).get(1), 0, 1, -2);
        assertEquals(3, graph.neighbors(2).size());
        assertEdge(graph.neighbors(2).get(1), 2, 2, -9);
        assertEdge(graph.neighbors(2).get(2), 2, 2, -9);
        assertTrue(graph.neighbors(5).isEmpty());
        assertTrue(graph.neighbors(6).isEmpty());
    }

    @Test
    void readsDirectedCycleWithoutAddingReverseEdges() throws IOException {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        assertTrue(graph.isDirected());
        assertEquals(6, graph.vertexCount());
        assertEquals(7, graph.edgeCount());
        assertEquals(7, adjacencyEntries(graph));
        assertEquals(1, graph.neighbors(0).size());
        assertEdge(graph.neighbors(0).get(0), 0, 1, 2);
        assertEdge(graph.neighbors(1).get(0), 1, 2, -3);
        assertEdge(graph.neighbors(2).get(0), 2, 0, 1);
        assertTrue(graph.neighbors(5).isEmpty());
    }

    @Test
    void preservesDirectedParallelEdgesAndLoop() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 1, -1);
        graph.addEdge(1, 1, 7);
        assertEquals(3, graph.edgeCount());
        assertEquals(3, adjacencyEntries(graph));
        assertEdge(graph.neighbors(0).get(1), 0, 1, -1);
        assertEdge(graph.neighbors(1).get(0), 1, 1, 7);
    }

    @Test
    void acceptsEmptyGraphAndIsolatedVertex() throws IOException {
        Graph empty = readText("0 0 directed\n");
        assertEquals(0, empty.vertexCount());
        assertEquals(0, empty.edgeCount());
        Graph singleton = readText("1 0 undirected\n");
        assertEquals(1, singleton.vertexCount());
        assertTrue(singleton.neighbors(0).isEmpty());
    }

    @Test
    void acceptsWhitespaceAndFullIntWeightRange() throws IOException {
        Graph graph = readText(" 2\t2 directed \n0 1 -2147483648\n1\t0 2147483647\n\n");
        assertEdge(graph.edges().get(0), 0, 1, Integer.MIN_VALUE);
        assertEdge(graph.edges().get(1), 1, 0, Integer.MAX_VALUE);
    }

    @Test
    void rejectsInvalidVerticesBeforeChangingGraph() {
        assertThrows(IllegalArgumentException.class, () -> new Graph(-1, false));
        Graph graph = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.neighbors(2));
        assertThrows(IllegalArgumentException.class, () -> graph.neighbors(-1));
        assertEquals(0, graph.edgeCount());
        assertEquals(0, adjacencyEntries(graph));
    }

    @Test
    void callersCannotChangeListsAndBreakGraphConsistency() {
        Graph graph = new Graph(2, false);
        graph.addEdge(0, 1, 2);
        assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.neighbors(0).clear());
        assertEquals(1, graph.edgeCount());
        assertEquals(2, adjacencyEntries(graph));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "\n", "2 1\n", "2 1 directed extra\n", "2 0 unknown\n",
            "-1 0 directed\n", "2 -1 directed\n", "x 0 directed\n",
            "2147483648 0 directed\n", "2 1 directed\n", "2 1 directed\n0 1\n",
            "2 1 directed\n0 1 4 extra\n", "2 1 directed\n0 1 weight\n",
            "2 1 directed\n0 1 2147483648\n", "2 1 directed\n-1 1 4\n",
            "2 1 directed\n0 2 4\n", "0 1 directed\n0 0 4\n",
            "2 0 directed\n0 1 4\n", "2 1 directed\n\n0 1 4\n"
    })
    void rejectsMalformedFilesWithAnExplanation(String input) {
        IOException exception = assertThrows(IOException.class, () -> readText(input));
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void reportsMissingFile() {
        assertThrows(IOException.class, () -> Graph.read(temporaryDirectory.resolve("missing.txt")));
    }

    private Graph readText(String text) throws IOException {
        Path file = temporaryDirectory.resolve("graph.txt");
        Files.writeString(file, text);
        return Graph.read(file);
    }

    private static int adjacencyEntries(Graph graph) {
        int count = 0;
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            count += graph.neighbors(vertex).size();
        }
        return count;
    }

    private static long totalInputWeight(Graph graph) {
        long total = 0;
        for (Edge edge : graph.edges()) {
            total += edge.weight;
        }
        return total;
    }

    private static void assertEdge(Edge edge, int from, int to, int weight) {
        assertEquals(from, edge.from);
        assertEquals(to, edge.to);
        assertEquals(weight, edge.weight);
    }
}
