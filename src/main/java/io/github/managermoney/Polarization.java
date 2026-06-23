package io.github.managermoney;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public enum Polarization {
    VERTICAL(Material.WHITE_CONCRETE),
    HORIZONTAL(Material.ORANGE_CONCRETE),
    LEFT_CIRCULAR(Material.PURPLE_CONCRETE),
    RIGHT_CIRCULAR(Material.CYAN_CONCRETE),
    ELLIPTICAL(Material.MAGENTA_CONCRETE);

    private static final List<Polarization> VALUES = Arrays.asList(values());
    private static final Random RANDOM = new Random();

    private final Material material;

    Polarization(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    /** Локализованное отображаемое имя. */
    public String getDisplayName() {
        return Lang.get("polarization." + name());
    }

    public Polarization next() {
        return VALUES.get((ordinal() + 1) % VALUES.size());
    }

    public Polarization previous() {
        return VALUES.get((ordinal() - 1 + VALUES.size()) % VALUES.size());
    }

    public static Polarization random() {
        return VALUES.get(RANDOM.nextInt(VALUES.size()));
    }
}
