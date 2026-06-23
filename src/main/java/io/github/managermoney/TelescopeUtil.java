package io.github.managermoney;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public final class TelescopeUtil {
    private TelescopeUtil() {}

    public static boolean isSkyClear(Block startBlock) {
        World world = startBlock.getWorld();
        int maxY = world.getMaxHeight();
        int currentY = startBlock.getY() + 1;
        while (currentY < maxY) {
            Block block = world.getBlockAt(startBlock.getX(), currentY, startBlock.getZ());
            if (!block.getType().isAir()) return false;
            currentY++;
        }
        return true;
    }

    public static String formatFrequency(Material mat) {
        int index = TelescopeGUI.FREQUENCY_TORCHES.indexOf(mat);
        List<String> freqs = Lang.getList("frequencies");
        String value = (index >= 0 && index < freqs.size())
                ? freqs.get(index)
                : Lang.get("format.unknown");
        return Lang.get("format.frequency", "value", value);
    }

    public static String formatDirection(Material mat) {
        int index = TelescopeGUI.DIRECTION_MATERIALS.indexOf(mat);
        List<String> dirs = Lang.getList("directions");
        String value = (index >= 0 && index < dirs.size())
                ? dirs.get(index)
                : Lang.get("format.unknown");
        return Lang.get("format.direction", "value", value);
    }

    public static String formatPolarization(Polarization polarization) {
        return Lang.get("format.polarization", "value", polarization.getDisplayName());
    }
}
