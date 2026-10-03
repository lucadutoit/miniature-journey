# S15 Codec Test

Android app (Java) that measures how much battery MP3, AAC, Opus and FLAC playback uses.
The app only collects the data — the statistics are done in Python (`analysis/analyse.py`).

## Start

1. Open this folder in Android Studio and wait for Gradle sync.
2. **View → Tool Windows → TODO** and filter by your tag to see your tasks.
3. You're done when your test passes (right-click it → **Run**).

## Who does what

| Tag | Job | Test |
| --- | --- | --- |
| `runner` | Run the trials and record the battery (`SessionRunner`) | `MetricsTest` |
| `protocol` | Trial order + outlier rule | `TrialSchedulerTest`, `OutlierEngineTest` |
| `setup` | New session screen + checklist | `PreflightCheckerTest` |
| `sessions` | Past sessions list + CSV export | `CsvTest` |
| `home` | Home screen battery info + testing on the real phone | try it on the phone |
| `analysis` | Python script: averages, ANOVA, Tukey, chart | run it on `sample_trials.csv` |

Use the **Quick test run** button on the home screen to try a session before the New Session screen is done.

## Rules

- Only edit your own files. Ask the group before changing anything in `db/` or `Config.java`.
- Don't use the database on the main thread — use `new Thread(() -> { ... }).start()`.
- Use your own branch and merge often.
