package com.s15.codectest;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.measure.BatteryMonitor;
import com.s15.codectest.measure.BatterySnapshot;
import com.s15.codectest.measure.RunnerStatus;

import java.util.Locale;

// Home screen. Owner: home (Michael)
public class MainActivity extends AppCompatActivity {

    private TextView phoneInfo;
    private TextView warning;
    private Button runningButton;

    private BatteryMonitor monitor;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            showPhoneInfo();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneInfo = findViewById(R.id.phoneInfo);
        warning = findViewById(R.id.warning);
        runningButton = findViewById(R.id.runningButton);
        monitor = new BatteryMonitor(this);

        findViewById(R.id.newSessionButton).setOnClickListener(v ->
                startActivity(new Intent(this, NewSessionActivity.class)));
        findViewById(R.id.sessionsButton).setOnClickListener(v ->
                startActivity(new Intent(this, SessionsActivity.class)));
        runningButton.setOnClickListener(v ->
                startActivity(new Intent(this, RunnerActivity.class)));

        // Needed so the "measurement running" notification can show on Android 13+.
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        runningButton.setVisibility(RunnerStatus.active ? View.VISIBLE : View.GONE);
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    private void showPhoneInfo() {
        BatterySnapshot phone = monitor.snapshot();

        // some phones give the current as negative, we only care about the size
        String current = phone.currentUa == null
                ? "not reported"
                : String.format(Locale.US, "%.1f mA", Math.abs(phone.currentUa) / 1000.0);

        phoneInfo.setText("Battery: " + phone.levelPct + " %"
                + "\nTemperature: " + String.format(Locale.US, "%.1f °C", phone.tempC())
                + "\nCurrent: " + current
                + "\nVoltage: " + String.format(Locale.US, "%.2f V", phone.voltageMv / 1000.0)
                + "\nAirplane mode: " + (phone.airplaneMode ? "on" : "off")
                + "\nCharger: " + (phone.plugged ? "plugged in" : "not plugged in"));

        // without current or charge counter we can't measure anything
        if (!monitor.hasCurrent() && !monitor.hasChargeCounter()) {
            warning.setText("This phone doesn't report current or charge, so it can't be used for the study.");
        } else {
            warning.setText("");
        }
    }
}
