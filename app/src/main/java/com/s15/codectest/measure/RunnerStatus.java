package com.s15.codectest.measure;

// What the session runner is doing right now. The runner writes these and
// RunnerActivity reads them every second to update the screen.
public class RunnerStatus {
    public static volatile boolean active;
    public static volatile long sessionId;
    public static volatile String message = "";
    public static volatile String clip = "";
    public static volatile long trialElapsedMs;
    public static volatile long trialDurationMs;
    public static volatile int trialsDone;
    public static volatile int trialsTotal;
    public static volatile Double currentMa;
    public static volatile Double tempC;
    public static volatile int violations;

    public static void start(long id) {
        sessionId = id;
        active = true;
        message = "Starting…";
        clip = "";
        trialElapsedMs = 0;
        trialDurationMs = 0;
        trialsDone = 0;
        trialsTotal = 0;
        currentMa = null;
        tempC = null;
        violations = 0;
    }

    public static void finish(String finalMessage) {
        active = false;
        message = finalMessage;
    }
}
