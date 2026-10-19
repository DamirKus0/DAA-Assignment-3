package benchmark;

import algorithms.Kosaraju;
import algorithms.SccResult;
import algorithms.Tarjan;
import graph.Graph;
import metrics.Metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BonusBenchmark {
    private BonusBenchmark() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: BonusBenchmark [output.csv]");
        }
        Path output = Path.of(args.length == 0 ? "results/bonus.csv" : args[0]);
        List<String> rows = new ArrayList<>();
        rows.add(Benchmark.HEADER);
        for (int n : new int[]{1000, 3162, 10000, 31623, 100000}) {
            for (int density : new int[]{4, 20}) {
                Graph graph = GraphGenerator.generate(n, density * n, "directed");
                Metrics kosarajuMetrics = new Metrics();
                SccResult reference = Kosaraju.compute(graph, kosarajuMetrics);
                ReferenceValues.check("Kosaraju", n, graph.edgeCount(), reference.componentCount());
                Metrics metrics = new Metrics();
                long[] times = new long[5];
                int depth = -1;
                SccResult actual = null;
                for (int run = 0; run < 5; run++) {
                    long start = System.nanoTime();
                    actual = Tarjan.compute(graph, metrics);
                    times[run] = System.nanoTime() - start;
                    if (!reference.samePartitionAs(actual) || metrics.edgeScans != graph.edgeCount()
                            || kosarajuMetrics.edgeScans != 2L * graph.edgeCount()) {
                        throw new IllegalStateException("Tarjan/Kosaraju mismatch");
                    }
                    if (depth != -1 && depth != metrics.maxDepth) {
                        throw new IllegalStateException("Unstable Tarjan depth");
                    }
                    depth = metrics.maxDepth;
                }
                Arrays.sort(times);
                String row = String.format(Locale.ROOT, "Tarjan,directed_%s,%d,%d,%.6f,0,%d,0,%d,%d",
                        density == 4 ? "sparse" : "dense", n, graph.edgeCount(), times[2] / 1_000_000.0,
                        metrics.edgeScans, metrics.maxDepth, actual.componentCount());
                rows.add(row);
                System.out.println(row);
            }
        }
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, rows);
        System.out.println("Verified all bonus SCC results; wrote " + (rows.size() - 1) + " rows to " + output);
    }
}
