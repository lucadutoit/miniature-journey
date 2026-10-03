package com.s15.codectest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.s15.codectest.measure.BatterySnapshot;
import com.s15.codectest.measure.PreflightChecker;

import org.junit.Test;

import java.util.List;

// Setup: PreflightChecker.problems
public class PreflightCheckerTest {

    private BatterySnapshot phone(int batteryPct, boolean plugged, boolean airplane) {
        return new BatterySnapshot(-200_000L, 3_000_000L, 4000, batteryPct, 300, plugged, false, airplane);
    }

    @Test
    public void readyPhoneHasNoProblems() {
        assertTrue(PreflightChecker.problems(phone(90, false, true)).isEmpty());
    }

    @Test
    public void airplaneModeOff() {
        List<String> problems = PreflightChecker.problems(phone(90, false, false));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).toLowerCase().contains("airplane"));
    }

    @Test
    public void chargerPluggedIn() {
        List<String> problems = PreflightChecker.problems(phone(90, true, true));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).toLowerCase().contains("charger"));
    }

    @Test
    public void batteryTooLow() {
        List<String> problems = PreflightChecker.problems(phone(Config.MIN_BATTERY_PERCENT - 1, false, true));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).toLowerCase().contains("battery"));
    }

    @Test
    public void everythingWrongGivesThreeProblems() {
        assertEquals(3, PreflightChecker.problems(phone(50, true, false)).size());
    }
}
