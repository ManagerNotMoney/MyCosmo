package io.github.managermoney;

import org.bukkit.Material;

public class FrequencyDirection {

    private final Material frequency;
    private final Material direction;
    private final Polarization polarization;

    public FrequencyDirection(Material frequency, Material direction) {
        this(frequency, direction, Polarization.VERTICAL);
    }

    public FrequencyDirection(Material frequency, Material direction, Polarization polarization) {
        this.frequency = frequency;
        this.direction = direction;
        this.polarization = polarization == null ? Polarization.VERTICAL : polarization;
    }

    public Material getFrequency() {
        return frequency;
    }

    public Material getDirection() {
        return direction;
    }

    public Polarization getPolarization() {
        return polarization;
    }
}
