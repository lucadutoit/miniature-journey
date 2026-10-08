package com.s15.codectest.measure;

import com.s15.codectest.Config;
import java.util.ArrayList;
import java.util.List;

// Owner: setup
public class PreflightChecker {

    public static List<String> problems(BatterySnapshot phone) {
        List<String> problems = new ArrayList<>();

        if (!phone.airplaneMode) {
            problems.add("Turn on airplane mode.");
        }
        if (phone.plugged) {
            problems.add("Unplug the charger.");
        }
        if (phone.levelPct < Config.MIN_BATTERY_PERCENT) {
            problems.add("Charge the battery to at least " + Config.MIN_BATTERY_PERCENT + "%");
        }
        return problems;
    }
}

