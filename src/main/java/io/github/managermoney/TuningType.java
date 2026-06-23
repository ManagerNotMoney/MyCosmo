package io.github.managermoney;

public enum TuningType {
    FREQUENCY,
    POLARIZATION,
    DIRECTION,
    CALIBRATION;

    public String getDisplay() {
        return Lang.get("tuning_type." + name());
    }
}
