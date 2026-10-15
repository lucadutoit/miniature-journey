package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: protocol
public class OutlierEngine {

    // The positions of values that are more than `sdLimit` standard deviations away from the others.
    //
    // "Leave-one-out": each value is compared against the mean and sample SD (divide by n - 1) of
    // all the OTHER values. This matters because with only 5 trials a value that is included in its
    // own mean and SD can never be more than 1.79 SD away, so the 3 SD rule could never trigger.
    // (Worth mentioning in the report.)
    public static List<Integer> findOutliers(List<Double> values, double sdLimit) {
        List<Integer> outliers = new ArrayList<>();
        if (values == null || values.size() < 3) return outliers;

        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);

            // mean of the other values
            double sum = 0;
            for (int j = 0; j < values.size(); j++) {
                if (j != i) sum += values.get(j);
            }
            int othersCount = values.size() - 1;
            double mean = sum / othersCount;

            // sample SD of the other values (n - 1)
            double squares = 0;
            for (int j = 0; j < values.size(); j++) {
                if (j != i) squares += (values.get(j) - mean) * (values.get(j) - mean);
            }
            double sd = Math.sqrt(squares / (othersCount - 1));

            if (sd > 0 && Math.abs(value - mean) > sdLimit * sd) outliers.add(i);
        }
        return outliers;
    }
}
