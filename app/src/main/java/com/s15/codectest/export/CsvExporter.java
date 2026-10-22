package com.s15.codectest.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.s15.codectest.Clip;
import com.s15.codectest.db.BatterySample;
import com.s15.codectest.db.LabDao;
import com.s15.codectest.db.LabDatabase;
import com.s15.codectest.db.LogEntry;
import com.s15.codectest.db.Trial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Exports session data as CSV files in a shareable ZIP archive
// Owner: sessions
public class CsvExporter {

    // Column names the Python analysis script (analysis/analyse.py) expects in trials.csv.
    public static final String TRIAL_COLUMNS =
            "trial_id,session_id,clip,codec,kbps,repetition,is_rerun,status,delta_charge_mah,"
                    + "energy_joules,mean_current_ma,start_temp_c,end_temp_c,sample_count,note";

    public static final String SAMPLE_COLUMNS =
            "sample_id,trial_id,elapsed_ms,current_ua,charge_counter_uah,voltage_mv,level_pct,"
                    + "temp_c,screen_on,airplane_mode,plugged";

    public static final String LOG_COLUMNS = "time,trial_id,message";

    // Saves the session's trials, samples and log as one zip file in the cache and returns it.
    // Method is run on a background thread because it accesses the database
    public static File export(Context context, long sessionId) throws IOException {
        LabDao db = LabDatabase.get(context);

        File folder = new File(context.getCacheDir(), "exports");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not make the exports folder");
        }
        File zipFile = new File(folder, "s15-codec-test-session-" + sessionId + ".zip");

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))) {
            write(zip, "trials.csv", trialsCsv(db, sessionId));
            write(zip, "samples.csv", samplesCsv(db, sessionId));
            write(zip, "log.csv", logCsv(db, sessionId));
        }
        return zipFile;
    }

    private static String trialsCsv(LabDao db, long sessionId) {
        StringBuilder out = new StringBuilder(TRIAL_COLUMNS).append("\n");
        for (Trial t : db.getTrials(sessionId)) {
            Clip clip = Clip.find(t.clipFile);
            out.append(csv(t.id)).append(",")
                    .append(csv(t.sessionId)).append(",")
                    .append(csv(t.clipFile)).append(",")
                    .append(csv(clip == null ? null : clip.codec)).append(",")
                    .append(csv(clip == null ? null : clip.kbps)).append(",")
                    .append(csv(t.repetition)).append(",")
                    .append(csv(t.isRerun)).append(",")
                    .append(csv(t.status)).append(",")
                    .append(csv(t.deltaChargeMah)).append(",")
                    .append(csv(t.energyJoules)).append(",")
                    .append(csv(t.meanCurrentMa)).append(",")
                    .append(csv(tempC(t.startTempTenths))).append(",")
                    .append(csv(tempC(t.endTempTenths))).append(",")
                    .append(csv(t.sampleCount)).append(",")
                    .append(csv(t.note)).append("\n");
        }
        return out.toString();
    }

    private static String samplesCsv(LabDao db, long sessionId) {
        StringBuilder out = new StringBuilder(SAMPLE_COLUMNS).append("\n");
        for (BatterySample s : db.getSamples(sessionId)) {
            out.append(csv(s.id)).append(",")
                    .append(csv(s.trialId)).append(",")
                    .append(csv(s.elapsedMs)).append(",")
                    .append(csv(s.currentUa)).append(",")
                    .append(csv(s.chargeCounterUah)).append(",")
                    .append(csv(s.voltageMv)).append(",")
                    .append(csv(s.levelPct)).append(",")
                    .append(csv(tempC(s.tempTenths))).append(",")
                    .append(csv(s.screenOn)).append(",")
                    .append(csv(s.airplaneMode)).append(",")
                    .append(csv(s.plugged)).append("\n");
        }
        return out.toString();
    }

    private static String logCsv(LabDao db, long sessionId) {
        StringBuilder out = new StringBuilder(LOG_COLUMNS).append("\n");
        for (LogEntry e : db.getLog(sessionId)) {
            out.append(csv(e.time)).append(",")
                    .append(csv(e.trialId)).append(",")
                    .append(csv(e.message)).append("\n");
        }
        return out.toString();
    }


    //Adds a CSV file to the ZIP archive using UTF-8 encoding
    private static void write(ZipOutputStream zip, String name, String contents) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(contents.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // Converts temp from tenths of a °C to °C, null stays as is
    private static Double tempC(Integer tenths) {
        if (tenths == null) return null;
        return tenths / 10.0;
    }

    // Makes a value safe for a CSV cell by quoting fields containing commas, quotes or line breaks
    // Null values become empty cells, and existing quotes are doubled
    public static String csv(Object value) {
        if (value == null) return "";
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    // Opens the Android share menu (email, Google Drive, WhatsApp...) for the exported ZIP file
    public static void share(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share session data"));
    }
}
