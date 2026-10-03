package com.s15.codectest;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.MeasurementService;

import java.util.List;

// Pick the clips, check the phone is ready, start the session. Owner: setup
public class NewSessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);

        // TODO(setup):
        //  1. Add a CheckBox to R.id.clipList for every clip in Clip.ALL (text = clip.label()).
        //     Tick the 128k ones and FLAC by default.
        //  2. Every 2 seconds, run PreflightChecker.problems(new BatteryMonitor(this).snapshot())
        //     and show the problems in R.id.problemsText (or "Phone is ready" if there are none).
        //  3. Only enable R.id.startButton when there are no problems, both checklist boxes are
        //     ticked and at least 2 clips are ticked.
        //  4. When Start is pressed call startSession(name, tickedClipFiles, pilotChecked).
    }

    // Creates the session, starts the measurement and opens the running screen.
    private void startSession(String name, List<String> clipFiles, boolean pilot) {
        new Thread(() -> {
            long id = SessionMaker.create(this, name, clipFiles, pilot);
            runOnUiThread(() -> {
                MeasurementService.start(this, id);
                startActivity(new Intent(this, RunnerActivity.class));
                finish();
            });
        }).start();
    }
}
