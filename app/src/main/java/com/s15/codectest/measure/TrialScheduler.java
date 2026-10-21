package com.s15.codectest.measure;

import java.util.ArrayList;
import java.util.List;

// Owner: protocol
public class TrialScheduler {

    // Rotated Latin square, so no codec is always first or last: repetition 0 plays A B C D,
    // repetition 1 plays B C D A, repetition 2 plays C D A B... After as many repetitions as
    // there are clips the rotation starts over again.
    // The whole session's play order comes back as one list.
    public static List<String> buildOrder(List<String> clips, int repetitions) {
        List<String> order = new ArrayList<>();
        if (clips == null || clips.isEmpty()) return order;

        for (int rep = 0; rep < repetitions; rep++) {
            int shift = rep % clips.size();
            for (int i = 0; i < clips.size(); i++) {
                order.add(clips.get((i + shift) % clips.size()));
            }
        }
        return order;
    }
}
