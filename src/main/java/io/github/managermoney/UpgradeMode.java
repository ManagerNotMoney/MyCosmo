package io.github.managermoney;

public enum UpgradeMode {
    NONE,
    DECODER,
    DENOISE,
    EQUALIZE,
    DYNAMIC_RANGE,
    MASTERING;

    /** Локализованное отображаемое имя режима. */
    public String getDisplayName() {
        return Lang.get("upgrade_mode." + name());
    }

    /**
     * Строка-качество дискеты, которое требуется для этого режима.
     * Возвращает ключ из DisketteManager.QUALITIES или null.
     */
    public String getRequiredQuality() {
        switch (this) {
            case DENOISE:       return Lang.get("diskette_quality.terrible"); // "Ужасное" / "Terrible"
            case EQUALIZE:      return Lang.get("diskette_quality.bad");
            case DYNAMIC_RANGE: return Lang.get("diskette_quality.average");
            case MASTERING:     return Lang.get("diskette_quality.good");
            default:            return null;
        }
    }

    public UpgradeMode next() {
        UpgradeMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public UpgradeMode previous() {
        UpgradeMode[] values = values();
        int index = ordinal() - 1;
        if (index < 0) index = values.length - 1;
        return values[index];
    }

    /**
     * Найти режим, необходимый для апгрейда дискеты данного качества.
     */
    public static UpgradeMode getRequiredMode(String currentQuality) {
        for (UpgradeMode mode : values()) {
            String req = mode.getRequiredQuality();
            if (req != null && req.equals(currentQuality)) {
                return mode;
            }
        }
        return NONE;
    }
}
