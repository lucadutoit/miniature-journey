package com.s15.codectest.measure;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.s15.codectest.R;

// Runs the session in the background so it keeps going with the screen off.
public class MeasurementService extends Service {

    private Thread runnerThread;
    private PowerManager.WakeLock wakeLock;

    public static void start(Context context, long sessionId) {
        Intent intent = new Intent(context, MeasurementService.class);
        intent.putExtra("sessionId", sessionId);
        ContextCompat.startForegroundService(context, intent);
    }

    // Stopping the service aborts the session.
    public static void stop(Context context) {
        context.stopService(new Intent(context, MeasurementService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (runnerThread != null && runnerThread.isAlive()) return START_NOT_STICKY;
        long sessionId = intent.getLongExtra("sessionId", -1);

        showNotification();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "S15CodecTest:session");
        wakeLock.acquire(12 * 60 * 60 * 1000L);

        runnerThread = new Thread(() -> {
            try {
                new SessionRunner(this).run(sessionId);
            } catch (InterruptedException e) {
                Log.i("MeasurementService", "Session aborted");
            } catch (Exception e) {
                Log.e("MeasurementService", "Session crashed", e);
                RunnerStatus.finish("Error: " + e.getMessage());
            }
            stopSelf();
        });
        runnerThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (runnerThread != null) runnerThread.interrupt();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showNotification() {
        NotificationChannel channel = new NotificationChannel("session", "Measurement session", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(this, "session")
                .setSmallIcon(R.drawable.ic_stat_lab)
                .setContentTitle("Measurement running")
                .setContentText("Keep the screen off until the session finishes.")
                .setOngoing(true)
                .build();
        ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }
}
