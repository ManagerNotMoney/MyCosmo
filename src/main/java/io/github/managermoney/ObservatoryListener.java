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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ObservatoryListener implements Listener {

    private final Set<Location>               builtObservatories = new HashSet<>();
    private final ObservatoryGUI              gui;
    private final JavaPlugin                  plugin;
    private final File                        dataFile;
    private final Map<Location, ItemStack[]>  savedContents      = new HashMap<>();

    public ObservatoryListener(ObservatoryGUI gui, JavaPlugin plugin) {
        this.gui      = gui;
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "observa.yml");
        loadObservatories();
    }

    private boolean isObservatory(Block bottomCenter) {
        if (bottomCenter.getType() != Material.LIGHT_BLUE_GLAZED_TERRACOTTA) return false;
        World w = bottomCenter.getWorld();
        int x = bottomCenter.getX(), y = bottomCenter.getY(), z = bottomCenter.getZ();

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

    public ItemStack[] getSavedContents(Block obsBlock) {
        return savedContents.get(obsBlock.getLocation());
    }

    public void setSavedContents(Block obsBlock, ItemStack[] contents) {
        Location loc = obsBlock.getLocation();
        if (contents != null) {
            boolean any = false;
            for (ItemStack item : contents) if (item != null) { any = true; break; }
            if (any) { savedContents.put(loc, contents); return; }
        }
        savedContents.remove(loc);
    }

    private void loadObservatories() {
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> list = cfg.getMapList("observatories");
        for (Map<?, ?> map : list) {
            try {
                String worldName = (String) map.get("world");
                int x = ((Number) map.get("x")).intValue();
                int y = ((Number) map.get("y")).intValue();
                int z = ((Number) map.get("z")).intValue();
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) continue;
                Block block = world.getBlockAt(x, y, z);
                if (!isObservatory(block)) continue;
                Location loc = block.getLocation().clone();
                builtObservatories.add(loc);

                List<Object> s3 = (List<Object>) map.get("slot3");
                List<Object> s4 = (List<Object>) map.get("slot4");
                List<Object> s5 = (List<Object>) map.get("slot5");
                ItemStack[] contents = new ItemStack[3];
                if (s3 != null && !s3.isEmpty() && s3.get(0) instanceof Map)
                    contents[0] = ItemStack.deserialize((Map<String, Object>) s3.get(0));
                if (s4 != null && !s4.isEmpty() && s4.get(0) instanceof Map)
                    contents[1] = ItemStack.deserialize((Map<String, Object>) s4.get(0));
                if (s5 != null && !s5.isEmpty() && s5.get(0) instanceof Map)
                    contents[2] = ItemStack.deserialize((Map<String, Object>) s5.get(0));
                if (contents[0] != null || contents[1] != null || contents[2] != null)
                    savedContents.put(loc, contents);

            } catch (Exception ex) {
                plugin.getLogger().warning(Lang.get("plugin.obs_load_err", "error", ex.getMessage()));
            }
        }
        plugin.getLogger().info(Lang.get("plugin.obs_loaded", "count", builtObservatories.size()));
    }

    public void saveObservatories() {
        FileConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Location loc : builtObservatories) {
            Map<String, Object> map = new HashMap<>();
            map.put("world", loc.getWorld().getName());
            map.put("x", loc.getBlockX());
            map.put("y", loc.getBlockY());
            map.put("z", loc.getBlockZ());
            ItemStack[] contents = savedContents.get(loc);
            if (contents != null) {
                if (contents[0] != null) map.put("slot3", Collections.singletonList(contents[0].serialize()));
                if (contents[1] != null) map.put("slot4", Collections.singletonList(contents[1].serialize()));
                if (contents[2] != null) map.put("slot5", Collections.singletonList(contents[2].serialize()));
            }
            list.add(map);
        }
        cfg.set("observatories", list);
        try { cfg.save(dataFile); }
        catch (IOException e) {
            plugin.getLogger().warning(Lang.get("plugin.obs_save_err", "error", e.getMessage()));
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
                    if (builtObservatories.contains(candidate.getLocation())) continue;
                    if (isObservatory(candidate)) {
                        builtObservatories.add(candidate.getLocation().clone());
                        saveObservatories();
                        player.sendMessage(Lang.get("observatory.built"));
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
                    if (builtObservatories.contains(loc)) {
                        if (!isObservatory(candidate)) continue;
                        builtObservatories.remove(loc);
                        savedContents.remove(loc);
                        saveObservatories();
                        player.sendMessage(Lang.get("observatory.destroyed"));
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
        if (clicked.getType() == Material.LIGHT_BLUE_GLAZED_TERRACOTTA
                && builtObservatories.contains(clicked.getLocation())) {
            event.setCancelled(true);
            gui.open(event.getPlayer(), clicked);
        }
    }
}
