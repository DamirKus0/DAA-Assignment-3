package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CriticalPath {
    private CriticalPath() {
    }

    public static Result compute(Condensation condensation) {
        return compute(condensation, new Metrics());
    }

    public static Result compute(Condensation condensation, Metrics metrics) {
        metrics.reset();
        Graph dag = condensation.graph();
        int[] order = Kahn.compute(dag);
        int n = dag.vertexCount();
        boolean[] hasIncoming = new boolean[n];
        for (Edge edge : dag.edges()) {
            hasIncoming[edge.to] = true;
        }
        long[] finish = new long[n];
        Arrays.fill(finish, Long.MIN_VALUE);
        int[] parent = new int[n];
        Arrays.fill(parent, -1);
        for (int vertex = 0; vertex < n; vertex++) {
            if (!hasIncoming[vertex]) {
                finish[vertex] = condensation.durationOf(vertex);
            }
        }
        for (int vertex : order) {
            if (finish[vertex] == Long.MIN_VALUE) {
                continue;
            }
            for (Edge edge : dag.neighbors(vertex)) {
                metrics.edgeScans++;
                long candidate = finish[vertex] + condensation.durationOf(edge.to);
                metrics.comparisons++;
                if (candidate > finish[edge.to]) {
                    finish[edge.to] = candidate;
                    parent[edge.to] = vertex;
                }
            }
        }
        int end = -1;
        for (int vertex = 0; vertex < n; vertex++) {
            if (end == -1) {
                end = vertex;
            } else {
                metrics.comparisons++;
                if (finish[vertex] > finish[end]) {
                    end = vertex;
                }
            }
        }
        return new Result(finish, parent, end);
    }

    public static final class Result {
        private final long[] finish;
        private final int[] parent;
        private final int end;

        private Result(long[] finish, int[] parent, int end) {
            this.finish = finish.clone();
            this.parent = parent.clone();
            this.end = end;
        }

        public long duration() {
            return end == -1 ? 0 : finish[end];
        }

        public long[] finishTimes() {
            return finish.clone();
        }

        public List<Integer> componentPath() {
            List<Integer> path = new ArrayList<>();
            for (int vertex = end; vertex != -1; vertex = parent[vertex]) {
                path.add(vertex);
            }
            Collections.reverse(path);
            return List.copyOf(path);
        }
    }
}
