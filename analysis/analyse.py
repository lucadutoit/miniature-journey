# S15 Codec Test - data analysis
# owner: analysis
#
# Setup (once):   pip install pandas scipy matplotlib
# Run:            python analyse.py sample_trials.csv
#
# sample_trials.csv is FAKE practice data. Once we have real results, export a session from
# the app (Past sessions -> tap it), unzip it, and run this on the real trials.csv

import sys

import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats

# Load the trial data from the file or use the sample data by default
file = sys.argv[1] if len(sys.argv) > 1 else "sample_trials.csv"
trials = pd.read_csv(file)

# Only completed trials are included in the statistical analysis. OUTLIER / FAILED / ABORTED rows
# stay in the file for the report but are left out of the statistics (proposal: flagged, never deleted)
done = trials[trials["status"] == "COMPLETED"]
print(len(done), "completed trials out of", len(trials))

if done.empty:
    print("No completed trials in this file, so there is nothing to analyse.")
    sys.exit(0)

MEASURE = "delta_charge_mah"

# "clip_aac_128.m4a" -> "AAC 128k", the same label the app shows (FLAC has no bitrate).
def clip_label(clip):
    row = done.loc[done["clip"] == clip].iloc[0]
    codec, kbps = row["codec"], row["kbps"]

    # Fall back to the clip filename if its codec information is missing
    if pd.isna(codec):
        return str(clip)
    return codec if pd.isna(kbps) or int(kbps) == 0 else f"{codec} {int(kbps)}k"


# Task 1: Calculate the number of trials, mean, standard deviation and coefficient of variation

# Sorting by mean ranks clips from lowest to highest average battery usage
summary = done.groupby("clip")[MEASURE].agg(["count", "mean", "std"]).sort_values("mean")
summary["cv_percent"] = summary["std"] / summary["mean"] * 100

clips = list(summary.index)
labels = [clip_label(clip) for clip in clips]
groups = [done.loc[done["clip"] == clip, MEASURE].tolist() for clip in clips]

# ANOVA and Tukey HSD require at least two clips with two completed trials each
enough_data = len(groups) >= 2 and all(len(g) >= 2 for g in groups)

print()
print("Battery used per clip")
print(f"{'clip':<12}{'n':>4}{'mean mAh':>11}{'SD':>9}{'CV%':>8}")
for label, (_, row) in zip(labels, summary.iterrows()):
    print(f"{label:<12}{int(row['count']):>4}{row['mean']:>11.3f}{row['std']:>9.3f}{row['cv_percent']:>8.2f}")


# Task 2: one-way ANOVA tests whether the mean battery usage differs across clips
print()
if not enough_data:
    print("Not enough data for an ANOVA (need at least 2 clips with 2 completed trials each).")
else:
    f_stat, p_value = stats.f_oneway(*groups)
    print(f"One-way ANOVA: F = {f_stat:.3f}, p = {p_value:.5g}")
    if p_value < 0.05:
        print("p < 0.05, so the clips do not all use the same amount of battery.")
    else:
        print("p >= 0.05, so no difference between the clips was found.")

# Task 3: Tukey HSD - to identify which pairs of clips differ after overall comparison
if enough_data:
    tukey = stats.tukey_hsd(*groups)
    print()
    print("Tukey HSD (every pair, p < 0.05 counts as a real difference)")
    print(tukey)
    print()
    for i in range(len(clips)):
        for j in range(i + 1, len(clips)):
            difference = tukey.statistic[i][j]
            p = tukey.pvalue[i][j]
            verdict = "different" if p < 0.05 else "no difference"
            print(f"  {labels[i]:<12} vs {labels[j]:<12} {difference:+8.3f} mAh  p = {p:.5g}  -> {verdict}")


# Task 4: Plot mean battery usage for each clip, using standard deviation as error bars
# keeps a consistent bar colour to focus on the measured differences
SURFACE = "#fcfcfb"
INK = "#0b0b0b"
INK_SOFT = "#52514e"
BLUE = "#2a78d6"

figure, axes = plt.subplots(figsize=(8, 5), facecolor=SURFACE)
axes.set_facecolor(SURFACE)

axes.bar(
    labels,
    summary["mean"],
    yerr=summary["std"],
    capsize=4,
    color=BLUE,
    width=0.6,
    ecolor=INK_SOFT,
    error_kw={"linewidth": 1.2},
)

axes.set_xlabel("Clip (codec and bitrate)", color=INK_SOFT)
axes.set_ylabel("Battery used per trial (mAh)", color=INK_SOFT)

axes.set_title(
    "Mean battery used per clip (error bars = 1 SD)",
    color=INK,
    fontsize=13,
    pad=14,
)

# Recessive grid and axes
axes.yaxis.grid(True, color="#e6e5e1", linewidth=0.8)
axes.set_axisbelow(True)
axes.spines["top"].set_visible(False)
axes.spines["right"].set_visible(False)
axes.spines["left"].set_visible(False)
axes.spines["bottom"].set_color("#cfcec9")
axes.tick_params(colors=INK_SOFT, length=0)

# Display the mean above each bar while allowing for the error bar height
headroom = axes.get_ylim()[1] * 0.02

for position, (mean, sd) in enumerate(zip(summary["mean"], summary["std"].fillna(0))):
    axes.text(
        position,
        mean + sd + headroom,
        f"{mean:.2f}",
        ha="center",
        va="bottom",
        color=INK,
        fontsize=9,
    )

figure.tight_layout()
figure.savefig("figure1.png", dpi=200, facecolor=SURFACE)
print()
print("Saved the chart as figure1.png")


# Task 5: Rank clips from lowest to highest mean battery usage
print()
print("Clips ranked from least to most battery used")

best = summary["mean"].iloc[0]  # summary is already sorted least to most

for place, (label, mean) in enumerate(zip(labels, summary["mean"]), start=1):
    extra = (mean - best) / best * 100 if best else 0.0
    print(f"  {place}. {label:<12}{mean:>8.3f} mAh  (+{extra:.1f}% vs the best)")
