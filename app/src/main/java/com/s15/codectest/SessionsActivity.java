package com.s15.codectest;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.s15.codectest.db.LabDatabase;
import com.s15.codectest.db.Session;

import java.util.List;

// List of past sessions; tap one to export it. Owner: sessions
public class SessionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);
        TextView infoText = findViewById(R.id.infoText);

        new Thread(() -> {
            List<Session> sessions = LabDatabase.get(this).getSessions();
            runOnUiThread(() -> infoText.setText("TODO(sessions): show these " + sessions.size()
                    + " sessions in the list below"));
        }).start();

        // TODO(sessions):
        //  1. Show the sessions in R.id.sessionList using an ArrayAdapter<String>
        //     (one line each, e.g. "Quick test - COMPLETED - 03 Oct 14:30").
        //  2. When a session is tapped: on a background thread call CsvExporter.export(this, session.id),
        //     then on the UI thread call CsvExporter.share(this, file). Show a Toast if it fails.
    }
}
