# S15 Codec Test

Android app (Java) that measures how much battery MP3, AAC, Opus and FLAC playback uses.
The app only collects the data — the statistics are done in Python (`analysis/analyse.py`).

## Demo Video

https://github.com/user-attachments/assets/d916f236-ea95-4fe8-9685-249e966b11e6



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

To try the app quickly, start a session from **New session** with **Pilot** ticked (2-minute trials, 2 repetitions).

## Building from a terminal

Android Studio uses its own JDK, so nothing is needed there. For `./gradlew` in a terminal, `JAVA_HOME`
must be a JDK 17–21 (newer ones are too new for this Gradle version). On Windows, Android Studio's
bundled one works:

```
./gradlew testDebugUnitTest -Dorg.gradle.java.home="C:/Program Files/Android/Android Studio/jbr"
```

## Demo Requirements
- Battery must be at least 80%
- Airplane mode must be enabled
- more than 1 codec option must be chosen

## Rules

- Only edit your own files. Ask the group before changing anything in `db/` or `Config.java`.
- Don't use the database on the main thread — use `new Thread(() -> { ... }).start()`.
- Use your own branch and merge often.


