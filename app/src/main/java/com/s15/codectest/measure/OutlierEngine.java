package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: protocol
public class OutlierEngine {

    // Identifies values that exceed the specified number of standard deviations
    // from the remaining values, using leave-one-out comparison
    public static List<Integer> findOutliers(List<Double> values, double sdLimit) {
        List<Integer> outliers = new ArrayList<>();

        // 3 values are needed to calculate the comparison
        if (values == null || values.size() < 3) return outliers;

        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);

            // Calc mean of all values except current one
            double sum = 0;
            for (int j = 0; j < values.size(); j++) {
                if (j != i) sum += values.get(j);
            }

            int othersCount = values.size() - 1;
            double mean = sum / othersCount;

            // sample SD of the remaining values (n - 1)
            double squares = 0;
            for (int j = 0; j < values.size(); j++) {
                if (j != i) {
                    squares += (values.get(j) - mean) * (values.get(j) - mean);
                }
            }

            double sd = Math.sqrt(squares / (othersCount - 1));

            // Record the index if the deviation exceeds the threshold
            if (sd > 0 && Math.abs(value - mean) > sdLimit * sd) {
                outliers.add(i);
            }
        }

        return outliers;
    }
}

