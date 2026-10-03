package com.s15.codectest;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.MeasurementService;
import com.s15.codectest.measure.RunnerStatus;

import java.util.ArrayList;
import java.util.List;

// Home screen. Owner: home
public class MainActivity extends AppCompatActivity {

    private TextView phoneInfo;
    private TextView warning;
    private Button runningButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneInfo = findViewById(R.id.phoneInfo);
        warning = findViewById(R.id.warning);
        runningButton = findViewById(R.id.runningButton);

        findViewById(R.id.newSessionButton).setOnClickListener(v ->
                startActivity(new Intent(this, NewSessionActivity.class)));
        findViewById(R.id.sessionsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SessionsActivity.class)));
        runningButton.setOnClickListener(v ->
                startActivity(new Intent(this, RunnerActivity.class)));
        findViewById(R.id.quickTestButton).setOnClickListener(v -> quickTest());

        // Needed so the "measurement running" notification can show on Android 13+.
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        // TODO(home): use BatteryMonitor to fill in phoneInfo, and refresh it every 2 seconds
        //  (Handler + postDelayed, start in onResume, stop in onPause). Show: battery %, temperature,
        //  current in mA (or "not reported"), airplane mode on/off, charger plugged in or not.
        //  If !hasCurrent() && !hasChargeCounter(), put a warning in `warning` that this phone
        //  can't be used for the study.
    }

    @Override
    protected void onResume() {
        super.onResume();
        runningButton.setVisibility(RunnerStatus.active ? View.VISIBLE : View.GONE);
    }

    // For testing the runner without the New Session screen: a 2-minute pilot with one clip per codec.
    // TODO: remove this button before handing in.
    private void quickTest() {
        new Thread(() -> {
            List<String> clips = new ArrayList<>();
            for (Clip c : Clip.ALL) {
                if (c.kbps == 128 || c.kbps == 0) clips.add(c.file);
            }
            long id = SessionMaker.create(this, "Quick test", clips, true);
            runOnUiThread(() -> {
                MeasurementService.start(this, id);
                startActivity(new Intent(this, RunnerActivity.class));
            });
        }).start();
    }
}
