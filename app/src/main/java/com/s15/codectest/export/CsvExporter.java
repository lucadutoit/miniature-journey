package com.s15.codectest.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

// Owner: sessions
public class CsvExporter {

    // Column names the Python analysis script (analysis/analyse.py) expects in trials.csv.
    public static final String TRIAL_COLUMNS =
            "trial_id,session_id,clip,codec,kbps,repetition,is_rerun,status,delta_charge_mah,"
                    + "energy_joules,mean_current_ma,start_temp_c,end_temp_c,sample_count,note";

    // TODO(sessions): save the session's data as a zip file and return it.
    //  - Put it in new File(context.getCacheDir(), "exports") (make the folder first),
    //    named "s15-codec-test-session-<id>.zip". Use java.util.zip.ZipOutputStream.
    //  - trials.csv: TRIAL_COLUMNS, then one row per trial (db.getTrials). Get codec/kbps from
    //    Clip.find(trial.clipFile). Temperatures in °C (tenths / 10.0). Empty cell for null values.
    //  - samples.csv: one row per BatterySample (db.getSamples).
    //  - log.csv: time, trial_id, message (db.getLog).
    //  - Put every cell through csv(...).
    //  Uses the database, so call it from a background thread.
    public static File export(Context context, long sessionId) throws IOException {
        throw new IOException("Export not built yet");
    }

    // TODO(sessions): make a value safe for a CSV cell (CsvTest checks this).
    //  null -> "". If the text has a comma, a quote or a new line, put quotes around it
    //  and double any quotes inside: say "hi"  ->  "say ""hi"""
    public static String csv(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    // Opens the Android share menu (email, Google Drive, WhatsApp...) for the exported file.
    public static void share(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share session data"));
    }
}
