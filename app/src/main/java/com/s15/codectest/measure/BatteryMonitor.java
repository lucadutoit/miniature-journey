package com.s15.codectest.measure;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;

// Reads the battery using Android's BatteryManager (no root or extra hardware needed).
public class BatteryMonitor {

    private final Context context;
    private final BatteryManager batteryManager;
    private final PowerManager powerManager;

    public BatteryMonitor(Context context) {
        this.context = context.getApplicationContext();
        batteryManager = (BatteryManager) this.context.getSystemService(Context.BATTERY_SERVICE);
        powerManager = (PowerManager) this.context.getSystemService(Context.POWER_SERVICE);
    }

    public BatterySnapshot snapshot() {
        Long current = null;
        if (hasCurrent()) current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        Long charge = null;
        if (hasChargeCounter()) charge = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        boolean plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

        boolean airplane = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

        return new BatterySnapshot(current, charge, voltage, level * 100 / scale, temp,
                plugged, powerManager.isInteractive(), airplane);
    }

    // Some phones don't report these (they return 0 or Long.MIN_VALUE).
    public boolean hasCurrent() {
        long value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        return value != 0 && value != Long.MIN_VALUE;
    }

    public boolean hasChargeCounter() {
        long value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        return value > 0;
    }
}
