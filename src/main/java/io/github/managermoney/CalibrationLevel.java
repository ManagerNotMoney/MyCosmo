package io.github.managermoney;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum CalibrationLevel {
    NOT_CALIBRATED(Material.RED_TERRACOTTA,  1.55),
    TERRIBLE       (Material.RED_CONCRETE,   1.35),
    BAD            (Material.YELLOW_TERRACOTTA, 1.20),
    INACCURATE     (Material.YELLOW_CONCRETE,1.05),
    NORMAL         (Material.GREEN_CONCRETE, 1.00),
    EXCELLENT      (Material.LIME_CONCRETE,  0.85);

    private static final List<CalibrationLevel> VALUES = Arrays.asList(values());

    private final Material material;
    private final double scanDelayMultiplier;

    CalibrationLevel(Material material, double scanDelayMultiplier) {
        this.material = material;
        this.scanDelayMultiplier = scanDelayMultiplier;
    }

    public Material getMaterial() { return material; }

    /** Локализованное отображаемое имя. */
    public String getDisplayName() {
        return Lang.get("calibration." + name());
    }

    public double getScanDelayMultiplier() { return scanDelayMultiplier; }

    public boolean isBelowInaccurate() {
        return ordinal() < INACCURATE.ordinal();
    }

    public CalibrationLevel next() {
        CalibrationLevel[] vals = values();
        if (ordinal() >= vals.length - 1) return this;
        return vals[ordinal() + 1];
    }

    public CalibrationLevel previous() {
        return VALUES.get((ordinal() - 1 + VALUES.size()) % VALUES.size());
    }
}
