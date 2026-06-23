package io.github.managermoney;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DisketteConsoleListener implements Listener {

    private final Set<Location>            builtConsoles = new HashSet<>();
    private final DisketteConsoleGUI       gui;
    private final JavaPlugin               plugin;
    private final File                     dataFile;
    private final Map<Location, UpgradeMode> savedModes  = new HashMap<>();

    public DisketteConsoleListener(DisketteConsoleGUI gui, JavaPlugin plugin) {
        this.gui      = gui;
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "console.yml");
        loadConsoles();
    }

    private boolean isConsole(Block center) {
        if (center.getType() != Material.DIAMOND_BLOCK) return false;
        World w = center.getWorld();
        int x = center.getX(), y = center.getY(), z = center.getZ();

        if (w.getBlockAt(x - 1, y, z).getType() == Material.REDSTONE_BLOCK &&
                w.getBlockAt(x + 1, y, z).getType() == Material.REDSTONE_BLOCK &&
                w.getBlockAt(x - 1, y + 1, z).getType() == Material.IRON_BLOCK &&
                w.getBlockAt(x,     y + 1, z).getType() == Material.IRON_BLOCK &&
                w.getBlockAt(x + 1, y + 1, z).getType() == Material.IRON_BLOCK) return true;

        if (w.getBlockAt(x, y, z - 1).getType() == Material.REDSTONE_BLOCK &&
                w.getBlockAt(x, y, z + 1).getType() == Material.REDSTONE_BLOCK &&
                w.getBlockAt(x, y + 1, z - 1).getType() == Material.IRON_BLOCK &&
                w.getBlockAt(x, y + 1, z    ).getType() == Material.IRON_BLOCK &&
                w.getBlockAt(x, y + 1, z + 1).getType() == Material.IRON_BLOCK) return true;

        return false;
    }

    public void setMode(Block block, UpgradeMode mode) {
        Location loc = block.getLocation();
        if (mode == UpgradeMode.NONE) savedModes.remove(loc);
        else savedModes.put(loc, mode);
    }

    public UpgradeMode getMode(Block block) {
        return savedModes.getOrDefault(block.getLocation(), UpgradeMode.NONE);
    }

    private void loadConsoles() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> list = cfg.getMapList("consoles");
        for (Map<?, ?> map : list) {
            try {
                String worldName = (String) map.get("world");
                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) continue;
                Block block = world.getBlockAt(x, y, z);
                if (!isConsole(block)) continue;
                Location loc = block.getLocation().clone();
                builtConsoles.add(loc);
                String modeName = (String) map.get("mode");
                if (modeName != null) {
                    try { savedModes.put(loc, UpgradeMode.valueOf(modeName)); }
                    catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception ex) {
                plugin.getLogger().warning(Lang.get("plugin.console_load_err", "error", ex.getMessage()));
            }
        }
        plugin.getLogger().info(Lang.get("plugin.console_loaded", "count", builtConsoles.size()));
    }

    public void saveConsoles() {
        FileConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Location loc : builtConsoles) {
            Map<String, Object> map = new HashMap<>();
            map.put("world", loc.getWorld().getName());
            map.put("x", loc.getBlockX());
            map.put("y", loc.getBlockY());
            map.put("z", loc.getBlockZ());
            UpgradeMode mode = savedModes.get(loc);
            if (mode != null && mode != UpgradeMode.NONE) map.put("mode", mode.name());
            list.add(map);
        }
        cfg.set("consoles", list);
        try { cfg.save(dataFile); }
        catch (IOException e) {
            plugin.getLogger().warning(Lang.get("plugin.console_save_err", "error", e.getMessage()));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block candidate = placed.getWorld().getBlockAt(
                            placed.getX() + dx, placed.getY() + dy, placed.getZ() + dz);
                    if (builtConsoles.contains(candidate.getLocation())) continue;
                    if (isConsole(candidate)) {
                        builtConsoles.add(candidate.getLocation().clone());
                        saveConsoles();
                        player.sendMessage(Lang.get("console.built"));
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Player player = event.getPlayer();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block candidate = broken.getWorld().getBlockAt(
                            broken.getX() + dx, broken.getY() + dy, broken.getZ() + dz);
                    Location loc = candidate.getLocation();
                    if (builtConsoles.contains(loc)) {
                        if (!isConsole(candidate)) continue;
                        builtConsoles.remove(loc);
                        savedModes.remove(loc);
                        if (gui != null) gui.clearStoredDiskette(candidate);
                        saveConsoles();
                        player.sendMessage(Lang.get("console.destroyed"));
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (clicked.getType() == Material.DIAMOND_BLOCK && builtConsoles.contains(clicked.getLocation())) {
            event.setCancelled(true);
            gui.open(event.getPlayer(), clicked);
        }
    }
}
