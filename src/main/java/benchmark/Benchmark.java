package benchmark;

import algorithms.*;
import graph.Graph;
import metrics.Metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Benchmark {
    public static final String HEADER = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
    private static final int RUNS = 5;

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: Benchmark [output.csv]");
        }
        Path output = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (int n : new int[]{1000, 3162, 10000, 31623, 100000}) {
            for (int density : new int[]{4, 20}) {
                lines.addAll(configuration(n, density * n));
            }
        }
        if (lines.size() != 51) {
            throw new IllegalStateException("Expected exactly 50 data rows");
        }
        Files.createDirectories(output.toAbsolutePath().getParent());
        // Publish results only after all algorithms and all references have passed.
        Files.write(output, lines);
        String environment = "java.version=" + System.getProperty("java.version")
                + "\njava.vendor=" + System.getProperty("java.vendor")
                + "\nos.name=" + System.getProperty("os.name")
                + "\nos.arch=" + System.getProperty("os.arch")
                + "\nprocessors=" + Runtime.getRuntime().availableProcessors()
                + "\nmax_heap_bytes=" + Runtime.getRuntime().maxMemory()
                + "\nruns=5\nwarmup_runs=0\nseed=42\n";
        Files.writeString(output.resolveSibling("environment.txt"), environment);
        System.out.println("Verified all PDF references; wrote 50 rows to " + output);
    }

    static List<String> configuration(int n, int m) {
        List<String> rows = new ArrayList<>();
        String density = m == 4 * n ? "sparse" : "dense";
        Graph graph = GraphGenerator.generate(n, m, "undirected");
        Run kruskal = measure("Kruskal", graph);
        Run prim = measure("Prim", graph);
        if (kruskal.result != prim.result) {
            throw new IllegalStateException("Kruskal and Prim weights disagree");
        }
        rows.add(row("Kruskal", "undirected_" + density, graph, kruskal));
        rows.add(row("Prim", "undirected_" + density, graph, prim));
        graph = GraphGenerator.generate(n, m, "directed");
        rows.add(row("Kosaraju", "directed_" + density, graph, measure("Kosaraju", graph)));
        graph = GraphGenerator.generate(n, m, "dag");
        rows.add(row("Kahn", "dag_" + density, graph, measure("Kahn", graph)));
        rows.add(row("DagShortestPath", "dag_" + density, graph, measure("DagShortestPath", graph)));
        return rows;
    }

    private static Run measure(String algorithm, Graph graph) {
        Run[] runs = new Run[RUNS];
        for (int i = 0; i < RUNS; i++) {
            Run run = execute(algorithm, graph);
            ReferenceValues.check(algorithm, graph.vertexCount(), graph.edgeCount(), run.result);
            checkCounters(algorithm, graph, run.metrics);
            if (i > 0 && !sameCounters(runs[0].metrics, run.metrics)) {
                throw new IllegalStateException("Counters changed between runs of " + algorithm);
            }
            runs[i] = run;
        }
        Arrays.sort(runs, (first, second) -> Long.compare(first.nanoseconds, second.nanoseconds));
        return runs[RUNS / 2];
    }

    private static Run execute(String algorithm, Graph graph) {
        Metrics metrics = new Metrics();
        long start = System.nanoTime();
        long elapsed;
        long result;
        switch (algorithm) {
            case "Kruskal", "Prim" -> {
                MstResult forest = algorithm.equals("Kruskal")
                        ? Kruskal.compute(graph, metrics) : Prim.compute(graph, metrics);
                elapsed = System.nanoTime() - start;
                result = forest.totalWeight();
                if (forest.components() != 1 || forest.edges().size() != graph.vertexCount() - 1) {
                    throw new IllegalStateException("Generated connected graph must have one spanning tree");
                }
            }
            case "Kosaraju" -> {
                SccResult components = Kosaraju.compute(graph, metrics);
                elapsed = System.nanoTime() - start;
                result = components.componentCount();
            }
            case "Kahn" -> {
                int[] order = Kahn.compute(graph, metrics);
                elapsed = System.nanoTime() - start;
                if (!Kahn.isValidOrder(graph, order)) {
                    throw new IllegalStateException("Invalid topological order");
                }
                result = order.length;
            }
            case "DagShortestPath" -> {
                PathResult paths = DagShortestPath.compute(graph, metrics);
                elapsed = System.nanoTime() - start;
                result = paths.sumDistances();
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
        return new Run(elapsed, result, metrics);
    }

    private static void checkCounters(String algorithm, Graph graph, Metrics metrics) {
        long m = graph.edgeCount();
        long expectedScans = switch (algorithm) {
            case "Kosaraju", "Prim" -> 2 * m;
            case "Kahn", "DagShortestPath" -> m;
            default -> -1;
        };
        if (expectedScans >= 0 && metrics.edgeScans != expectedScans) {
            throw new IllegalStateException("Wrong edge scan count for " + algorithm);
        }
        if (algorithm.equals("Kruskal") && metrics.edgeScans > m) {
            throw new IllegalStateException("Kruskal scanned too many candidates");
        }
        if (metrics.maxDepth < 0 || metrics.maxDepth > graph.vertexCount()) {
            throw new IllegalStateException("Invalid stack depth");
        }
    }

    private static boolean sameCounters(Metrics a, Metrics b) {
        return a.dsuOps == b.dsuOps && a.edgeScans == b.edgeScans
                && a.comparisons == b.comparisons && a.maxDepth == b.maxDepth;
    }

    private static String row(String algorithm, String kind, Graph graph, Run run) {
        Metrics metrics = run.metrics;
        String line = String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d",
                algorithm, kind, graph.vertexCount(), graph.edgeCount(), run.nanoseconds / 1_000_000.0,
                metrics.dsuOps, metrics.edgeScans, metrics.comparisons, metrics.maxDepth, run.result);
        System.out.println(line);
        return line;
    }

    private static final class Run {
        private final long nanoseconds;
        private final long result;
        private final Metrics metrics;

        private Run(long nanoseconds, long result, Metrics metrics) {
            this.nanoseconds = nanoseconds;
            this.result = result;
            this.metrics = metrics;
        }
    }
}
