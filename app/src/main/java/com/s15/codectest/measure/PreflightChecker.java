package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: setup
public class PreflightChecker {

    // TODO(setup): return a list of problems that stop the session from starting
    //  (empty list = ready to go). PreflightCheckerTest checks this.
    //  - airplane mode off      -> "Turn on airplane mode"
    //  - charger plugged in     -> "Unplug the charger"
    //  - battery below Config.MIN_BATTERY_PERCENT -> "Charge the battery to at least 80%"
    public static List<String> problems(BatterySnapshot phone) {
        return new ArrayList<>();
    }
}
