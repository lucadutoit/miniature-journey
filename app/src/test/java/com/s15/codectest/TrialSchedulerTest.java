package com.s15.codectest;

import static org.junit.Assert.assertEquals;

import com.s15.codectest.measure.TrialScheduler;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

// Protocol: TrialScheduler.buildOrder
public class TrialSchedulerTest {

    private final List<String> clips = Arrays.asList("A", "B", "C", "D");

    @Test
    public void orderRotatesEveryRepetition() {
        List<String> order = TrialScheduler.buildOrder(clips, 4);
        assertEquals(Arrays.asList(
                "A", "B", "C", "D",
                "B", "C", "D", "A",
                "C", "D", "A", "B",
                "D", "A", "B", "C"), order);
    }

    @Test
    public void fifthRepetitionStartsOverAgain() {
        List<String> order = TrialScheduler.buildOrder(clips, 5);
        assertEquals(20, order.size());
        assertEquals(Arrays.asList("A", "B", "C", "D"), order.subList(16, 20));
    }
}
