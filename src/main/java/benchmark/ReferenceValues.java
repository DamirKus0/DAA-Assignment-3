package benchmark;

/** PDF page 4. Expected answers are used only to validate computed results. */
public final class ReferenceValues {
    private static final long[][] TABLE = {
            {1000, 4000, 147970, 27, 396274},
            {1000, 20000, 30363, 1, 252411},
            {3162, 12648, 468911, 137, 1271911},
            {3162, 63240, 97589, 1, 723157},
            {10000, 40000, 1486139, 373, 3941828},
            {10000, 200000, 309550, 1, 2381342},
            {31623, 126492, 4715515, 1188, 12551891},
            {31623, 632460, 968517, 1, 7376612},
            {100000, 400000, 15012094, 3923, 40096539},
            {100000, 2000000, 3056681, 1, 23512087}
    };

    private ReferenceValues() {
    }

    public static void check(String algorithm, int n, int m, long actual) {
        long expected = expected(algorithm, n, m);
        if (actual != expected) {
            throw new IllegalStateException(algorithm + " at n=" + n + ", m=" + m
                    + ": expected " + expected + ", computed " + actual);
        }
    }

    public static long expected(String algorithm, int n, int m) {
        for (long[] row : TABLE) {
            if (row[0] == n && row[1] == m) {
                return switch (algorithm) {
                    case "Kruskal", "Prim" -> row[2];
                    case "Kosaraju" -> row[3];
                    case "Kahn" -> n;
                    case "DagShortestPath" -> row[4];
                    default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
                };
            }
        }
        throw new IllegalArgumentException("No PDF reference for n=" + n + ", m=" + m);
    }
}
