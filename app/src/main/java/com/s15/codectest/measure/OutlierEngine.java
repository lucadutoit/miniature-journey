package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: protocol
public class OutlierEngine {

    // TODO(protocol): return the positions of values that are more than `sdLimit` standard
    //  deviations away from the others. OutlierEngineTest checks this.
    //  - Fewer than 3 values: return an empty list.
    //  - For each value, work out the mean and sample SD (divide by n - 1) of all the OTHER values
    //    ("leave-one-out"). Flag it if SD > 0 and |value - mean| > sdLimit * SD.
    //  Why leave-one-out: with only 5 trials, if a value is included in its own mean and SD it can
    //  never be more than 1.79 SD away, so the 3 SD rule could never trigger. Mention this in the report.
    public static List<Integer> findOutliers(List<Double> values, double sdLimit) {
        return new ArrayList<>();
    }
}
