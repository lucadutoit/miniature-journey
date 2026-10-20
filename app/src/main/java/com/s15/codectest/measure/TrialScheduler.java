package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: protocol
public class TrialScheduler {

    // TODO(protocol): the proposal says the codec order must shift systematically so no codec is
    //  always first or last. Use a "rotated Latin square": repetition 0 plays A B C D,
    //  repetition 1 plays B C D A, repetition 2 plays C D A B, and so on (rotate by rep % number of clips).
    //  Return the full play order for the session as one list. TrialSchedulerTest checks this.
    //
    //  Right now it just plays the clips in the same order every repetition (placeholder).
    public static List<String> buildOrder(List<String> clips, int repetitions) {
        List<String> order = new ArrayList<>();
        for (int rep = 0; rep < repetitions; rep++) {
            order.addAll(clips);
        }
        return order;
    }
}
