package com.s15.codectest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.s15.codectest.db.BatterySample;
import com.s15.codectest.measure.BatterySnapshot;
import com.s15.codectest.measure.SessionRunner;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

// Runner: SessionRunner.computeMetrics
public class MetricsTest {

    private BatterySnapshot phone(Long currentUa, Long chargeUah) {
        return new BatterySnapshot(currentUa, chargeUah, 4000, 80, 300, false, false, true);
    }

    private List<BatterySample> samples(int seconds, Long currentUa) {
        List<BatterySample> list = new ArrayList<>();
        for (int i = 1; i <= seconds; i++) {
            BatterySample s = new BatterySample();
            s.elapsedMs = i * 1000L;
            s.currentUa = currentUa;
            s.voltageMv = 4000;
            list.add(s);
        }
        return list;
    }

    @Test
    public void chargeUsedIsInMilliampHours() {
        SessionRunner.Metrics m = SessionRunner.computeMetrics(
                phone(-200_000L, 3_000_000L), new ArrayList<>(), phone(-200_000L, 2_950_000L));
        assertNotNull(m.deltaChargeMah);
        assertEquals(50.0, m.deltaChargeMah, 0.000001);
    }

    @Test
    public void constantCurrentGivesExpectedJoules() {
        // 200 mA at 4 V for 60 s = 0.2 * 4 * 60 = 48 J
        SessionRunner.Metrics m = SessionRunner.computeMetrics(
                phone(-200_000L, null), samples(60, -200_000L), phone(-200_000L, null));
        assertNotNull(m.energyJoules);
        assertEquals(48.0, m.energyJoules, 0.000001);
        assertEquals(200.0, m.meanCurrentMa, 0.000001);
    }

    @Test
    public void positiveCurrentWorksToo() {
        SessionRunner.Metrics m = SessionRunner.computeMetrics(
                phone(150_000L, null), samples(10, 150_000L), phone(150_000L, null));
        assertNotNull(m.meanCurrentMa);
        assertEquals(150.0, m.meanCurrentMa, 0.000001);
        assertEquals(6.0, m.energyJoules, 0.000001);
    }

    @Test
    public void nothingReportedGivesNulls() {
        SessionRunner.Metrics m = SessionRunner.computeMetrics(phone(null, null), samples(5, null), phone(null, null));
        assertNull(m.deltaChargeMah);
        assertNull(m.energyJoules);
        assertNull(m.meanCurrentMa);
    }
}
