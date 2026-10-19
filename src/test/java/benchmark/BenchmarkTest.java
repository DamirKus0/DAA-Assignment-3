package benchmark;

import algorithms.Kahn;
import algorithms.Kruskal;
import graph.Edge;
import graph.Graph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    @Test
    void generatorIsDeterministicAndRespectsGraphKinds() {
        for (String kind : new String[]{"undirected", "directed", "dag"}) {
            Graph first = GraphGenerator.generate(50, 200, kind);
            Graph second = GraphGenerator.generate(50, 200, kind);
            assertEquals(50, first.vertexCount());
            assertEquals(200, first.edgeCount());
            assertEquals(!kind.equals("undirected"), first.isDirected());
            for (int i = 0; i < first.edgeCount(); i++) {
                Edge a = first.edges().get(i);
                Edge b = second.edges().get(i);
                assertEquals(a.from, b.from);
                assertEquals(a.to, b.to);
                assertEquals(a.weight, b.weight);
                assertNotEquals(a.from, a.to);
                assertTrue(a.weight >= 1 && a.weight <= 1000);
            }
            if (kind.equals("dag")) {
                assertTrue(Kahn.isValidOrder(first, Kahn.compute(first)));
            } else if (kind.equals("undirected")) {
                assertEquals(1, Kruskal.compute(first).components());
            }
        }
    }

    @Test
    void rejectsImpossibleGeneratorRequests() {
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(0, 1, "dag"));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(1, 1, "directed"));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(5, 3, "undirected"));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(-1, 0, "directed"));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(5, -1, "dag"));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.generate(5, 10, "unknown"));
        assertEquals(0, GraphGenerator.generate(0, 0, "undirected").edgeCount());
    }

    @Test
    void referenceValidationFailsLoudly() {
        ReferenceValues.check("Kruskal", 1000, 4000, 147970);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ReferenceValues.check("Kruskal", 1000, 4000, 147971));
        assertTrue(error.getMessage().contains("147971"));
        assertThrows(IllegalArgumentException.class, () -> ReferenceValues.expected("Kahn", 10, 40));
        assertThrows(IllegalArgumentException.class, () -> ReferenceValues.expected("Other", 1000, 4000));
    }

    @Test
    void producesFiveValidatedCsvRowsForOneConfiguration() {
        List<String> rows = Benchmark.configuration(1000, 4000);
        assertEquals(5, rows.size());
        for (String row : rows) {
            String[] fields = row.split(",");
            assertEquals(10, fields.length);
            assertEquals("1000", fields[2]);
            assertEquals("4000", fields[3]);
            assertTrue(Double.parseDouble(fields[4]) >= 0);
            assertEquals(ReferenceValues.expected(fields[0], 1000, 4000), Long.parseLong(fields[9]));
        }
    }
}
