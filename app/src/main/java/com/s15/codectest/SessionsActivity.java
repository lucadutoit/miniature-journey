package com.s15.codectest;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.db.LabDatabase;
import com.s15.codectest.db.Session;
import com.s15.codectest.export.CsvExporter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// List of past sessions; tap one to export it. Owner: sessions
public class SessionsActivity extends AppCompatActivity {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());

    // Kept so the tap handler can look up which session was tapped.
    private final List<Session> sessions = new ArrayList<>();

    private TextView infoText;
    private ListView sessionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);
        infoText = findViewById(R.id.infoText);
        sessionList = findViewById(R.id.sessionList);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < sessions.size()) exportSession(sessions.get(position));
        });

        loadSessions();
    }

    // Re-read the list every time the screen comes back, so a session that just finished shows up.
    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }

    private void loadSessions() {
        new Thread(() -> {
            List<Session> loaded = LabDatabase.get(this).getSessions();
            runOnUiThread(() -> showSessions(loaded));
        }).start();
    }

    private void showSessions(List<Session> loaded) {
        sessions.clear();
        sessions.addAll(loaded);

        List<String> lines = new ArrayList<>();
        for (Session s : sessions) {
            lines.add(describe(s));
        }
        sessionList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines));

        infoText.setText(sessions.isEmpty()
                ? "No sessions yet. Start one from the home screen."
                : "Tap a session to export its data.");
    }

    // e.g. "Quick test (pilot) - COMPLETED - 03 Oct 14:30"
    private String describe(Session session) {
        String name = session.name == null || session.name.isEmpty() ? "Session " + session.id : session.name;
        return name + (session.pilot ? " (pilot)" : "")
                + " - " + session.status
                + " - " + dateFormat.format(new Date(session.createdAt));
    }

    private void exportSession(Session session) {
        Toast.makeText(this, "Exporting " + session.name + "…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                File file = CsvExporter.export(this, session.id);
                runOnUiThread(() -> CsvExporter.share(this, file));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
