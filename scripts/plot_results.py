"""Rebuild the three assignment plots and numerical slope/ratio summary from CSV."""
import argparse
import csv
import json
import math
from pathlib import Path
from statistics import mean

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


ALGORITHMS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
SIZES = [1000, 3162, 10000, 31623, 100000]
COLORS = ["#2166ac", "#b35806", "#1b7837", "#762a83", "#d73027"]
MARKERS = ["o", "s", "^", "o", "x"]


def slope(rows, field):
    xs = [math.log(row["n"]) for row in rows]
    ys = [math.log(row[field]) for row in rows]
    xmean, ymean = mean(xs), mean(ys)
    return sum((x - xmean) * (y - ymean) for x, y in zip(xs, ys)) / sum(
        (x - xmean) ** 2 for x in xs
    )


def read_rows(csv_path):
    with csv_path.open(newline="", encoding="utf-8") as file:
        rows = list(csv.DictReader(file))
    if len(rows) != 50:
        raise ValueError("Expected exactly 50 benchmark rows")
    keys = set()
    for row in rows:
        for field in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
            row[field] = int(row[field])
        row["time_ms"] = float(row["time_ms"])
        if row["algorithm"] not in ALGORITHMS or row["n"] not in SIZES:
            raise ValueError("Unexpected algorithm or size")
        if row["m"] not in (4 * row["n"], 20 * row["n"]):
            raise ValueError("Unexpected density")
        key = row["algorithm"], row["n"], row["m"]
        if key in keys:
            raise ValueError("Duplicate benchmark row")
        keys.add(key)
    return rows


def select(rows, algorithm, density):
    return sorted((r for r in rows if r["algorithm"] == algorithm and r["m"] == density * r["n"]),
                  key=lambda r: r["n"])


def decorate(axis, title, ylabel, logarithmic_y=False):
    axis.set_title(title)
    axis.set_xscale("log")
    if logarithmic_y:
        axis.set_yscale("log")
    axis.set_xlabel("Vertices n")
    axis.set_ylabel(ylabel)
    axis.grid(True, which="both", alpha=0.2)


def plot(csv_path, output):
    rows = read_rows(csv_path)
    output.mkdir(parents=True, exist_ok=True)
    summary = []
    plt.rcParams.update({"font.size": 10, "savefig.dpi": 180})
    figure, axes = plt.subplots(1, 2, figsize=(12, 5.3), layout="constrained")
    figure.suptitle("Measured work: weight comparisons for MST, edge scans for traversals")
    for axis, density in zip(axes, (4, 20)):
        for algorithm, color, marker in zip(ALGORITHMS, COLORS, MARKERS):
            data = select(rows, algorithm, density)
            counter = "comparisons" if algorithm in ("Kruskal", "Prim") else "edge_scans"
            axis.plot([r["n"] for r in data], [r[counter] for r in data],
                      color=color, marker=marker, markerfacecolor="none",
                      linestyle="--" if algorithm == "DagShortestPath" else "-", label=algorithm)
            ratios = [r[counter] / (r["m"] * math.log2(r["m"]) if counter == "comparisons"
                                  else r["n"] + r["m"]) for r in data]
            summary.append({
                "algorithm": algorithm, "density": density, "counter": counter,
                "operation_slope": slope(data, counter), "scan_slope": slope(data, "edge_scans"),
                "ratio_min": min(ratios), "ratio_max": max(ratios),
                "scans_per_edge": [r["edge_scans"] / r["m"] for r in data],
                "dsu_ops_per_edge": [r["dsu_ops"] / r["m"] for r in data],
                "time_ms": [r["time_ms"] for r in data],
            })
        decorate(axis, f"m = {density}n", "Count per run", logarithmic_y=True)
        axis.legend(fontsize=9)
    figure.savefig(output / "operations_vs_n.png")
    plt.close(figure)

    figure, axes = plt.subplots(1, 2, figsize=(12, 5.3), layout="constrained")
    figure.suptitle("Main-pass edge scans per input edge (preparation counted separately)")
    for axis, density in zip(axes, (4, 20)):
        for algorithm, color, marker in zip(ALGORITHMS, COLORS, MARKERS):
            data = select(rows, algorithm, density)
            axis.plot([r["n"] for r in data], [r["edge_scans"] / r["m"] for r in data],
                      color=color, marker=marker, markerfacecolor="none",
                      linestyle="--" if algorithm in ("Kosaraju", "DagShortestPath") else "-",
                      label=algorithm)
        decorate(axis, f"m = {density}n", "edge_scans / m")
        axis.set_ylim(0, 2.25)
        axis.legend(loc="center right", bbox_to_anchor=(1, 0.7), fontsize=9)
    figure.savefig(output / "scans_per_edge.png")
    plt.close(figure)

    figure, axis = plt.subplots(figsize=(8, 5), layout="constrained")
    for density, color in ((4, COLORS[0]), (20, COLORS[1])):
        data = select(rows, "Kruskal", density)
        axis.plot([r["n"] for r in data], [r["edge_scans"] / r["m"] for r in data],
                  marker="o", color=color, label=f"m = {density}n")
    decorate(axis, "Kruskal: fraction of sorted edges examined before connectivity", "edge_scans / m")
    axis.set_ylim(0, 1.05)
    axis.legend()
    figure.savefig(output / "kruskal_edge_share.png")
    plt.close(figure)
    summary_path = output.parent / "analysis.json"
    summary_path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote 3 PNG plots to {output} and summary to {summary_path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", type=Path, nargs="?", default=Path("results/results.csv"))
    parser.add_argument("--output", type=Path, default=Path("results/plots"))
    args = parser.parse_args()
    plot(args.csv, args.output)
