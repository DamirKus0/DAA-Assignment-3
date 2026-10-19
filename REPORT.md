# Graph algorithms: measured work and correctness

Author: `<NAME SURNAME>`; group: `<GROUP>`.
Repository: https://github.com/DamirKus0/DAA-Assignment-3

## 1. Experiment and validation

The implementation uses Java 17, adjacency lists and handwritten algorithms.
Each graph kind and configuration starts with a new `Random(42)`.
The sizes are 1,000, 3,162, 10,000, 31,623 and 100,000; densities are `m=4n`
and `m=20n`. Undirected graphs start with a shuffled random spanning tree.
Directed graphs use random edges only. DAGs use a shuffled **rank indexed by
vertex**, orienting each edge from lower to higher rank. When endpoints coincide,
the second endpoint is drawn again, before drawing the weight `1+nextInt(1000)`.
Parallel edges remain. All ten reference configurations match, confirming this
interpretation of the generator for the required inputs.

Each algorithm is executed five times on the same unchanged graph. The CSV stores
the median time and counters from that run; counter equality is checked across
all five runs. There is no separate warmup. Timing includes the complete algorithm
call, its preparation, allocation and result construction. Graph generation,
reference checking, topological-order validation, distance summation and CSV I/O
are outside timing. `results/environment.txt` records Microsoft JDK 17.0.18,
macOS/aarch64, eight available processors and a 2 GiB maximum Java heap.

The benchmark checks Kruskal/Prim agreement, one tree with `n-1` edges,
every topological-order constraint, reference answers and required scan counts.
It fails before writing the final CSV if any check fails. These are computed results:

| n | m | MST weight (both) | SCC count | Sum of DAG distances |
| ---: | ---: | ---: | ---: | ---: |
| 1,000 | 4,000 | 147,970 | 27 | 396,274 |
| 1,000 | 20,000 | 30,363 | 1 | 252,411 |
| 3,162 | 12,648 | 468,911 | 137 | 1,271,911 |
| 3,162 | 63,240 | 97,589 | 1 | 723,157 |
| 10,000 | 40,000 | 1,486,139 | 373 | 3,941,828 |
| 10,000 | 200,000 | 309,550 | 1 | 2,381,342 |
| 31,623 | 126,492 | 4,715,515 | 1,188 | 12,551,891 |
| 31,623 | 632,460 | 968,517 | 1 | 7,376,612 |
| 100,000 | 400,000 | 15,012,094 | 3,923 | 40,096,539 |
| 100,000 | 2,000,000 | 3,056,681 | 1 | 23,512,087 |

## 2. Complexity and what is counted

Here `n=V`, `m=E`. Extra space excludes the input graph but includes the result.

| Operation | Time | Extra space | Reason |
| --- | --- | --- | --- |
| DSU makeSet | O(n) | O(n) | Initialize parent and size arrays. |
| DSU find / union | Amortized O(α(n)) | O(1) per call, O(n) stored DSU | Path compression plus union by size; iterative find. |
| Kruskal | O(n + m log(m+1) + m α(n)) | O(n+m) | Copy/sort edges, initialize DSU, inspect candidates. |
| Prim, edge priority queue | O(n + m log(m+1)) | O(n+m) | Scan adjacency, insert/pop at most O(m) candidates. |
| Kosaraju | O(n+m) | O(n+m) | Two DFS passes, reverse graph and O(n) arrays. |
| Kahn | O(n+m) | O(n) | In-degrees, queue and output permutation. |
| DAG shortest paths | O(n+m) | O(n) | Kahn, source initialization and one relaxation pass. |

Kruskal's usual connected-graph bound is O(m log m). Prim here stores edges,
including stale candidates, so claiming O(n) queue storage would be incorrect.
Parallel edges also make O(m log m) the appropriate queue bound without assuming
a simple graph. Path reconstruction takes O(k) time and memory for k vertices.

`dsu_ops` counts valid find calls plus successful unions. Kruskal explicitly
checks both roots and then calls union, giving five DSU operations per accepted
edge and two per rejected candidate. Its `edge_scans` excludes copying/sorting
and stops when one component remains. Prim counts every adjacency entry: `2m`.
Their `comparisons` count actual weight-comparator calls, including stale-entry
heap work. Kosaraju counts `2m` during DFS, excluding the additional `m` edges
used to build the reverse graph. Kahn counts `m` during queue processing on a DAG,
excluding its initial in-degree pass. DAG shortest paths count `m` relaxations
and `m` comparisons of candidate path weights; three earlier edge passes are
separate (Kahn preparation, Kahn processing, source initialization). All preparation
remains inside the time/space bounds. `max_depth` is the maximum active DFS path
length in vertices across either Kosaraju pass, not allocated capacity or JVM
call depth; the other algorithms report zero. It depends on adjacency order.

## 3. Counters, slopes and plots

`scripts/plot_results.py` computes least-squares slopes of log(count) against
log(n), using all five sizes separately at each density. `results/analysis.json`
contains slopes, ratios, scan shares and times; no measurements are hand-entered
by the plotting script.

| Algorithm | Normalized counter | Range, m=4n | Range, m=20n | Operation slope, sparse / dense |
| --- | --- | --- | --- | --- |
| Kruskal | comparisons / (m log2 m) | 0.745–0.907 | 0.666–0.903 | 1.0545 / 1.0159 |
| Prim | comparisons / (m log2 m) | 1.829–1.934 | 1.825–1.890 | 1.1079 / 1.0903 |
| Kosaraju | edge_scans / (n+m) | 1.600 | 1.905 | 1.0000 / 1.0000 |
| Kahn | edge_scans / (n+m) | 0.800 | 0.952 | 1.0000 / 1.0000 |
| DAG paths | edge_scans / (n+m) | 0.800 | 0.952 | 1.0000 / 1.0000 |

Traversal ratios are exactly constant at each density. Prim's comparison ratio
is approximately stable; Kruskal's decreases rather than staying exactly constant.
The fixed 1,000-value weight range creates many ties at larger sizes, and the
comparison sort's measured behavior need not realize its worst-case upper bound.
These finite observations are compatible with the bounds, not an asymptotic proof.
Kruskal's `dsu_ops/m` ranges from 2.290 to 2.748 (sparse) and 0.565 to 0.894 (dense);
it reflects the inspected prefix, not the internal parent-pointer cost of find.
Counting calls alone does not experimentally prove the α(n) bound.

The actual **scan** slopes of Kruskal are **1.0550 / 1.1194**. They differ from
comparison slopes because the examined fraction changes; they are not evidence
of sorting's logarithmic factor. Prim and all three traversal scan slopes are 1.
Kruskal examines 77.05–99.92% of sparse edges, versus 20.74–37.18% of dense edges.
Early stopping therefore saves many dense-graph DSU checks, while sorting still
processes every edge. Prim scans `2m` adjacency entries and maintains its heap;
these are different units, so scan counts alone cannot establish a universal winner.

Three reproducible figures (linked separately to keep the report compact):

1. [Operations versus n](results/plots/operations_vs_n.png): log-log;
   **comparisons** for Kruskal/Prim, **edge_scans** for Kosaraju/Kahn/DAG paths.
   Each density has a panel and each algorithm a line; Kahn/DAG lines coincide.
2. [Scans per edge](results/plots/scans_per_edge.png): `edge_scans/m` versus n;
   Prim/Kosaraju coincide at 2, Kahn/DAG paths at 1.
3. [Kruskal edge share](results/plots/kruskal_edge_share.png): its `edge_scans/m`
   for both densities, revealing the early-stopping behavior.

## 4. Correctness arguments

**MST and the cut property.** Maintain a forest contained in some minimum spanning
forest. Consider a cut that does not split any already selected tree.
If a minimum tree does not contain a chosen lightest crossing edge, adding it
creates a cycle containing another edge across that cut. Replacing that other
edge cannot increase the weight, so an optimum containing the chosen edge exists.
Kruskal's next accepted edge is lightest across a cut separating its current DSU
component from the rest. Prim's next non-stale minimum candidate is lightest
across the visited/unvisited cut of its current connected component.
Repeating these safe choices yields an optimum per component; loops are never
selected, and parallel or negative edges do not invalidate the exchange argument.

**DAG shortest paths by induction.** Add a conceptual source with zero-cost edges
to every original in-degree-zero vertex. Initially those sources have correct
distance zero and all other distances are infinite. Assume all vertices before
v in a topological order have their final distances. Every incoming edge of v
starts at such an earlier vertex, so all candidates for a path ending at v have
already been considered. Taking their minimum therefore gives the true optimum
for v, completing the induction. Negative edges are safe because the order has
no backward dependencies or cycles; general graphs may also allow negative edges,
but a reachable negative cycle destroys finite shortest distances downstream.
Recording the improving predecessor and exact edge reconstructs a path attaining
the stored distance, even when parallel edges have different weights.

## 5. Choosing an algorithm and interpreting time

| Situation | Choice and reason |
| --- | --- |
| Sparse undirected edge list, minimum forest | Kruskal: sorting and DSU are direct; handles disconnected input. |
| Sparse undirected adjacency lists | Prim: grow from existing adjacency, with an edge heap. |
| Dense simple undirected graph with matrix | O(n²) matrix Prim can avoid sorting/heap overhead; it is not implemented here. |
| Directed graph, cycles possible, group mutual dependencies | Kosaraju: SCCs; MST and topological order do not solve this problem. |
| Schedule dependencies, detect impossible ordering | Kahn: returns a complete order or reports a cycle. |
| Shortest paths, directed acyclic input, negative weights possible | DAG relaxation: linear after topological ordering. |
| General directed graph with negative weights | Bellman–Ford with negative-cycle detection; test oracle only here. |

At n=100,000 and m=2,000,000 the recorded medians were approximately 312 ms
(Kruskal), 970 ms (Prim), 276 ms (Kosaraju), 52 ms (Kahn) and 100 ms (DAG paths).
At small sizes some times decrease as n grows, illustrating JIT and measurement
noise. There is no explicit warmup, algorithms run in a fixed order, and GC,
allocation, cache state, other processes and JVM/hardware affect medians.
Five runs reduce outlier sensitivity but do not make a rigorous timing comparison;
the stable operation counts are the main evidence. Comparator counts can also
change with a different JDK collection implementation, unlike the exact traversal
scan identities. The observed large-case advantage of Kruskal over this edge-heap
Prim is specific to this generator and implementation.

## 6. Requirement interpretations and reproduction

The detailed requirement specifies ten size/density configurations, each with
three graph kinds and five algorithm rows. This produces 50 rows, resolving the
deliverables' conflicting phrase “four generated graphs”; DSU is not a sixth row.
The requested claim that Kruskal edge scans themselves show sorting's log factor
is corrected above. Negative weights are not exclusive to DAGs. Iterative SCC
tests cover both a path and a cycle of 100,000 vertices; randomized tests use
independent DSU labels, exhaustive small MST search, BFS mutual reachability and
Bellman–Ford with identical source initialization.

Reproduce with `mvn test`, `mvn package`,
`java -Xmx2g -cp target/classes benchmark.Benchmark`, then
`python3 scripts/plot_results.py` (install `requirements-plots.txt` first).
The main conclusion is supported by exact scan identities and matched references:
the traversal work is linear, while sorting/heap comparisons capture the extra
MST work; neither one counter nor one laptop's timing replaces the algorithmic proof.
