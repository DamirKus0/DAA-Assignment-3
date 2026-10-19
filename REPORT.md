# Graph algorithms: measured work and correctness

Author: `<NAME SURNAME>`; group: `<GROUP>`.
Repository: https://github.com/DamirKus0/DAA-Assignment-3

## 1. Experiment and validation

The Java 17 implementation uses adjacency lists and handwritten algorithms.
Each graph starts with `new Random(42)`. Undirected graphs include the shuffled
spanning tree; directed graphs use random edges; DAGs orient edges by shuffled
vertex ranks. Coincident endpoints trigger another second-endpoint draw, then
weight `1+nextInt(1000)`. Parallel edges remain. Five sizes and two densities
produce the following ten configurations; all reference answers match.

Five runs use the same unchanged graph, without separate warmup. CSV stores
median time and one run's counters, checked equal across runs. Timing includes
algorithm preparation, allocation and result construction, excluding generation,
validation, distance summation and CSV I/O. `results/environment.txt` records
Microsoft JDK 17.0.18, macOS/aarch64, eight processors and a 2 GiB heap limit.

The benchmark validates MST agreement and structure, topological order, references
and scan identities before writing CSV. These results are computed, not substituted:

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

Kruskal's connected-graph bound simplifies to O(m log m). Prim's edge heap,
including stale/parallel candidates, needs O(m) storage and a log m factor.
Reconstructing a k-vertex path takes O(k) time and memory.

`dsu_ops` counts find calls plus successful unions: five per accepted Kruskal
edge, two per rejected candidate. Kruskal scans sorted candidates until connected;
copying/sorting is excluded. Prim scans `2m` adjacency entries. Their `comparisons`
count weight-comparator calls. Kosaraju scans `2m` in DFS, with another uncounted
`m` for reversal. Kahn scans `m` in its queue pass, excluding in-degree preparation.
DAG paths count `m` relaxations/comparisons, excluding three preparatory edge
passes (Kahn degrees, Kahn queue, source detection). Preparation stays inside
time/space bounds. Kosaraju's `max_depth` is the largest active DFS path across
both passes, in vertices; it depends on traversal order. Other mandatory
algorithms have no DFS stack and report zero.

## 3. Counters, slopes and plots

`scripts/plot_results.py` fits log(count) against log(n), separately per density,
and writes slopes, ratios and scan shares to `results/analysis.json`.

| Algorithm | Normalized counter | Range, m=4n | Range, m=20n | Operation slope, sparse / dense |
| --- | --- | --- | --- | --- |
| Kruskal | comparisons / (m log2 m) | 0.745–0.907 | 0.666–0.903 | 1.0545 / 1.0159 |
| Prim | comparisons / (m log2 m) | 1.829–1.934 | 1.825–1.890 | 1.1079 / 1.0903 |
| Kosaraju | edge_scans / (n+m) | 1.600 | 1.905 | 1.0000 / 1.0000 |
| Kahn | edge_scans / (n+m) | 0.800 | 0.952 | 1.0000 / 1.0000 |
| DAG paths | edge_scans / (n+m) | 0.800 | 0.952 | 1.0000 / 1.0000 |

Traversal ratios are constant. Prim's comparison ratio is approximately stable;
Kruskal's decreases. The fixed 1,000-value weight range creates many ties, and
sorting need not realize its worst-case bound. Finite observations are compatible
with the bounds, not an asymptotic proof.
Kruskal's `dsu_ops/m` ranges from 2.290 to 2.748 (sparse) and 0.565 to 0.894 (dense);
it reflects the inspected prefix, not the internal parent-pointer cost of find.
Counting calls alone does not experimentally prove the α(n) bound.

Kruskal's **scan** slopes are **1.0550 / 1.1194**, reflecting a changing examined
fraction, not sorting's log factor. All other scan slopes are 1. Kruskal examines
77.05–99.92% of sparse versus 20.74–37.18% of dense edges. Early stopping saves
DSU checks, but all edges are sorted. Prim scans `2m` and maintains a heap;
different counter units cannot establish a universal winner.

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

At n=100,000 and m=2,000,000 the final medians were approximately 257 ms
(Kruskal), 1,031 ms (Prim), 287 ms (Kosaraju), 40 ms (Kahn) and 84 ms (DAG paths).
Some small-case times decrease as n grows: there is no warmup, and JIT, GC,
caches, fixed execution order, background load and hardware affect medians.
Five runs reduce outlier sensitivity without making timing decisive.
Comparator counts can change with JDK collections; exact traversal scan
identities do not. Kruskal's observed advantage applies to this generator and
edge-heap Prim, not all inputs or implementations.

## 6. Requirement interpretations and reproduction

**Bonus Tarjan.** Iterative discovery/lowlink uses separate DFS and active-SCC
stacks. Tests cover 100 random graphs and 100,000-vertex paths/cycles; all ten
generated partitions match Kosaraju. Tarjan uses O(n+m) time and O(n) extra space,
scans m rather than 2m edges, and avoids graph reversal.

**Bonus critical path.** Each SCC is a block of duration equal to its vertex count;
dependencies have zero lag, ignoring original weights. Internal cycles must already
be resolved; contraction does not cure deadlock. Deduplicated condensation is a
DAG: a component cycle would imply one SCC. Topological max-relaxation, including
source durations, gives project makespan with unlimited parallel resources.
Tests use exhaustive paths; the benchmark checks sign-flipped shortest paths.
For c SCCs/e dependencies, the path costs O(c+e) time/O(c) space; hash-based
condensation takes expected O(n+m) time/space. Longest simple paths are NP-hard
on general graphs, while DAG order permits this dynamic program.
For the largest sparse graph, 3,923 SCCs yield 4,331 dependencies and critical
duration 96,085; the dense graph is one SCC of duration 100,000.
The separate 20-row `results/bonus.csv` labels original sizes; CriticalPath n/m
describe the condensation. Its timer includes condensation/path work, excluding
the separately measured SCC calculation.

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
