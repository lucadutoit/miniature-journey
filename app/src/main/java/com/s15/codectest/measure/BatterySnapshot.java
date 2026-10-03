package com.s15.codectest.measure;

// One reading of the battery and phone state at a moment in time.
public class BatterySnapshot {
    public Long currentUa;        // null if the phone doesn't report current
    public Long chargeCounterUah; // null if the phone doesn't report the charge counter
    public int voltageMv;
    public int levelPct;
    public int tempTenths;        // e.g. 312 = 31.2 °C
    public boolean plugged;
    public boolean screenOn;
    public boolean airplaneMode;

    public BatterySnapshot(Long currentUa, Long chargeCounterUah, int voltageMv, int levelPct, int tempTenths,
                           boolean plugged, boolean screenOn, boolean airplaneMode) {
        this.currentUa = currentUa;
        this.chargeCounterUah = chargeCounterUah;
        this.voltageMv = voltageMv;
        this.levelPct = levelPct;
        this.tempTenths = tempTenths;
        this.plugged = plugged;
        this.screenOn = screenOn;
        this.airplaneMode = airplaneMode;
    }

    public double tempC() {
        return tempTenths / 10.0;
    }
}
