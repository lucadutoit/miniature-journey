package com.s15.codectest;

// All the study settings in one place (from our proposal).
public class Config {
    public static final int TRIAL_MINUTES = 10;
    public static final int REPETITIONS = 5;
    public static final int COOLDOWN_MINUTES = 3;

    // Pilot run: a short test to check the readings are steady before the real sessions.
    public static final int PILOT_TRIAL_MINUTES = 2;
    public static final int PILOT_REPETITIONS = 2;
    public static final int PILOT_COOLDOWN_SECONDS = 30;

    public static final int SAMPLE_EVERY_SECONDS = 5;
    public static final int VOLUME_PERCENT = 70;
    public static final int MIN_BATTERY_PERCENT = 80;
    public static final double OUTLIER_SD = 3.0;
    public static final int MAX_RERUNS_PER_CLIP = 2;
    // Battery must cool back down to within 1.0 °C (10 tenths) of the starting temperature.
    public static final int TEMP_TOLERANCE_TENTHS = 10;
}
