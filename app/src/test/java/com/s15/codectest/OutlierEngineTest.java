package com.s15.codectest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.s15.codectest.measure.OutlierEngine;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// Protocol: OutlierEngine.findOutliers
public class OutlierEngineTest {

    @Test
    public void tooFewValuesGivesNoOutliers() {
        assertTrue(OutlierEngine.findOutliers(Arrays.asList(1.0, 100.0), 3.0).isEmpty());
    }

    @Test
    public void findsTheFarValue() {
        assertEquals(Collections.singletonList(9), OutlierEngine.findOutliers(
                Arrays.asList(10.0, 10.1, 9.9, 10.05, 9.95, 10.0, 10.1, 9.9, 10.0, 25.0), 3.0));
    }

    @Test
    public void worksWithOnlyFiveTrials() {
        assertEquals(Collections.singletonList(4), OutlierEngine.findOutliers(
                Arrays.asList(10.0, 10.2, 9.8, 10.1, 30.0), 3.0));
    }

    @Test
    public void normalSpreadGivesNoOutliers() {
        assertTrue(OutlierEngine.findOutliers(Arrays.asList(10.0, 11.0, 9.0, 10.5, 9.5, 10.2), 3.0).isEmpty());
    }

    @Test
    public void identicalValuesGiveNoOutliers() {
        assertTrue(OutlierEngine.findOutliers(Arrays.asList(5.0, 5.0, 5.0, 5.0), 3.0).isEmpty());
    }
}
