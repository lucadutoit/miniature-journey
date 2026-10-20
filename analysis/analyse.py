# S15 Codec Test - data analysis (owner: analysis)
#
# Setup (once):   pip install pandas scipy matplotlib
# Run:            python analyse.py sample_trials.csv
#
# sample_trials.csv is FAKE practice data. Once we have real results, export a session from
# the app (Past sessions -> tap it), unzip it, and run this on the real trials.csv.

import sys

import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats

file = sys.argv[1] if len(sys.argv) > 1 else "sample_trials.csv"
trials = pd.read_csv(file)

# Only finished trials count. OUTLIER / FAILED / ABORTED rows stay in the file for the report
# but are left out of the statistics (proposal: flagged, never deleted).
done = trials[trials["status"] == "COMPLETED"]
print(len(done), "completed trials out of", len(trials))

# TODO(analysis) 1: for each clip print n, mean, SD and CV% (= SD / mean * 100) of delta_charge_mah.
#   Hint: done.groupby("clip")["delta_charge_mah"].agg(["count", "mean", "std"])

# TODO(analysis) 2: one-way ANOVA - is there a difference between the codecs?
#   Make one list of delta_charge_mah values per clip, then stats.f_oneway(*lists). Print F and p.

# TODO(analysis) 3: Tukey HSD - which pairs of codecs are different?
#   stats.tukey_hsd(*lists) and print the result.

# TODO(analysis) 4: bar chart of the mean per clip with SD error bars (plt.bar(..., yerr=...)),
#   label the axes, and save it with plt.savefig("figure1.png").

# TODO(analysis) 5: print the clips ranked from least to most battery used.
