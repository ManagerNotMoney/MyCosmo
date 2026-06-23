package io.github.managermoney;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TelescopeListener implements Listener {

    private static final List<Material> TELESCOPE_MATERIALS = Arrays.asList(
            Material.BLUE_GLAZED_TERRACOTTA,
            Material.REDSTONE_BLOCK,
            Material.WAXED_COPPER_BARS,
            Material.WAXED_LIGHTNING_ROD,
            Material.REDSTONE_TORCH
    );

    private final Set<Location> builtTelescopes = new HashSet<>();
    private TelescopeGUI gui;
    private final JavaPlugin plugin;
    private final Map<Location, BukkitTask> scanningTasks = new HashMap<>();
    private static final Map<Location, FrequencyDirection> activeSignals = new HashMap<>();
    private final Map<Location, Integer> telescopeIds = new HashMap<>();
    private final Map<Location, BukkitTask> signalExpiryTasks = new HashMap<>();
    private static final long SIGNAL_LIFESPAN_TICKS = 6000L;

    private final NamespacedKey paperKey;

    public TelescopeListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.paperKey = new NamespacedKey(plugin, "telescope_id");
    }

    public void setGui(TelescopeGUI gui) {
        this.gui = gui;
    }

    public static Map<Location, FrequencyDirection> getActiveSignals() {
        return activeSignals;
    }

    public Integer getTelescopeId(Location loc) {
        return telescopeIds.get(loc);
    }

    public Location getLocationByTelescopeId(int id) {
        for (Map.Entry<Location, Integer> entry : telescopeIds.entrySet()) {
            if (entry.getValue() == id) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void registerTelescope(Block bottom) {
        int id;
        do {
            id = 100000 + ThreadLocalRandom.current().nextInt(900000);
        } while (telescopeIds.containsValue(id));
        Location loc = bottom.getLocation().clone();
        telescopeIds.put(loc, id);
        builtTelescopes.add(loc);
    }

    private void unregisterTelescope(Location loc) {
        telescopeIds.remove(loc);
        builtTelescopes.remove(loc);
    }

    private ItemStack createTelescopePaper(int id) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Номер Радиовышки");
            meta.setLore(Collections.singletonList("§7ID: " + id));
            meta.getPersistentDataContainer().set(paperKey, PersistentDataType.INTEGER, id);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    private boolean isTelescope(Block bottom) {
        if (bottom.getType() != TELESCOPE_MATERIALS.get(0)) return false;
        for (int i = 1; i < TELESCOPE_MATERIALS.size(); i++) {
            Block above = bottom.getWorld().getBlockAt(bottom.getX(), bottom.getY() + i, bottom.getZ());
            if (above.getType() != TELESCOPE_MATERIALS.get(i)) return false;
        }
        return true;
    }

    public void cancelScanning(Location loc) {
        BukkitTask task = scanningTasks.remove(loc);
        if (task != null) task.cancel();
    }

    public boolean isScanning(Location loc) {
        return scanningTasks.containsKey(loc);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();
        for (int offset = -4; offset <= 0; offset++) {
            Block bottomCandidate = placed.getWorld().getBlockAt(placed.getX(), placed.getY() + offset, placed.getZ());
            if (builtTelescopes.contains(bottomCandidate.getLocation())) continue;
            if (isTelescope(bottomCandidate)) {
                registerTelescope(bottomCandidate);
                player.sendMessage("Радиотелескоп построен");
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Player player = event.getPlayer();
        for (int i = 0; i < TELESCOPE_MATERIALS.size(); i++) {
            Block bottomCandidate = broken.getWorld().getBlockAt(broken.getX(), broken.getY() - i, broken.getZ());
            Location loc = bottomCandidate.getLocation();
            if (!builtTelescopes.contains(loc)) continue;

            player.sendMessage("Радиотелескоп разрушен");
            cancelSignalExpiry(loc);
            cancelScanning(loc);
            activeSignals.remove(loc);
            unregisterTelescope(loc);
            if (gui != null) {
                gui.removeTelescopeSettings(loc);
                gui.resetScanButton(loc);
            }
            return;
        }
    }

    public void registerScanTask(Location loc, BukkitTask task) {
        cancelScanning(loc);
        scanningTasks.put(loc, task);
    }

    public boolean isTelescopeExists(Location loc) {
        return builtTelescopes.contains(loc);
    }

    public void cancelSignalExpiry(Location loc) {
        BukkitTask task = signalExpiryTasks.remove(loc);
        if (task != null) task.cancel();
    }

    public void startSignalExpiry(Location loc) {
        cancelSignalExpiry(loc);
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSignals.containsKey(loc)) {
                activeSignals.remove(loc);
                Set<Player> viewers = gui != null ? new HashSet<>(gui.getLocationViewers(loc)) : Collections.emptySet();
                for (Player p : viewers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§cСигнал радиотелескопа истёк (прошло 5 минут).");
                    }
                }
                if (gui != null) gui.resetScanButton(loc);
            }
            signalExpiryTasks.remove(loc);
        }, SIGNAL_LIFESPAN_TICKS);
        signalExpiryTasks.put(loc, expiryTask);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        Player player = event.getPlayer();

        if (clicked.getType() == Material.BLUE_GLAZED_TERRACOTTA && builtTelescopes.contains(clicked.getLocation())) {
            if (gui != null) gui.open(player, clicked.getLocation());
            event.setCancelled(true);
            return;
        }

        if (clicked.getType() == Material.REDSTONE_BLOCK) {
            Block bottom = clicked.getWorld().getBlockAt(clicked.getX(), clicked.getY() - 1, clicked.getZ());
            Location loc = bottom.getLocation();
            if (!builtTelescopes.contains(loc)) return;
            event.setCancelled(true);

            ItemStack handItem = event.getItem();
            if (handItem != null && handItem.getType() == Material.PAPER) {
                Integer id = getTelescopeId(loc);
                if (id != null) {
                    if (handItem.getAmount() > 1) {
                        handItem.setAmount(handItem.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                    ItemStack paperId = createTelescopePaper(id);
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(paperId);
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    // Убрано дублирующее сообщение — оно было и здесь, и в старом коде
                    player.sendMessage("§aID радиовышки записан на бумагу.");
                } else {
                    player.sendMessage("§cУ этой радиовышки нет ID.");
                }
                return;
            }

            Block topBlock = bottom.getWorld().getBlockAt(bottom.getX(), bottom.getY() + 4, bottom.getZ());
            if (!TelescopeUtil.isSkyClear(topBlock)) {
                player.sendMessage("Обзор закрыт. Сигнал не может быть получен.");
                return;
            }

            if (activeSignals.containsKey(loc)) {
                player.sendMessage("У этого радиотелескопа уже есть активный сигнал. Запишите его на дискету.");
                return;
            }
            if (isScanning(loc)) {
                player.sendMessage("§cСканирование уже выполняется. Подождите.");
                return;
            }

            Material randomFreq = TelescopeGUI.FREQUENCY_TORCHES.get(ThreadLocalRandom.current().nextInt(TelescopeGUI.FREQUENCY_TORCHES.size()));
            Material randomDir = TelescopeGUI.DIRECTION_MATERIALS.get(ThreadLocalRandom.current().nextInt(TelescopeGUI.DIRECTION_MATERIALS.size()));
            Polarization randomPol = Polarization.random();
            FrequencyDirection signal = new FrequencyDirection(randomFreq, randomDir, randomPol);

            player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
            player.sendMessage("§eНачинается сканирование неба... Это займёт время.");
            cancelScanning(loc);

            int baseDelay = 300 + ThreadLocalRandom.current().nextInt(501);
            int delay = gui != null ? gui.getAdjustedScanDelay(loc, baseDelay) : baseDelay;

            BukkitTask scanTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!builtTelescopes.contains(loc)) {
                    player.sendMessage("§cТелескоп был разрушен во время сканирования.");
                    scanningTasks.remove(loc);
                    return;
                }
                if (activeSignals.containsKey(loc)) {
                    player.sendMessage("§cУ этого радиотелескопа уже есть активный сигнал.");
                    scanningTasks.remove(loc);
                    return;
                }
                if (gui != null && gui.shouldMissSignal(loc)) {
                    player.sendMessage("§cСканирование завершено. Сигнал не обнаружен.");
                    scanningTasks.remove(loc);
                    return;
                }
                activeSignals.put(loc, signal);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
                // Убрано старое сообщение — теперь только через GUI и подписку
                if (gui != null) {
                    gui.onSignalFound(loc, randomFreq, randomDir, randomPol);
                }
                startSignalExpiry(loc);
                scanningTasks.remove(loc);
            }, delay);
            scanningTasks.put(loc, scanTask);
        }
    }

    public boolean hasNearbyTelescope(Location loc, double radius) {
        double radiusSq = radius * radius;
        for (Location other : builtTelescopes) {
            if (other.equals(loc)) continue;
            if (other.getWorld() != null && loc.getWorld() != null
                    && other.getWorld().equals(loc.getWorld())
                    && other.distanceSquared(loc) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    public void finishScanning(Location loc) {
        BukkitTask task = scanningTasks.remove(loc);
        if (task != null) task.cancel();
    }

    public NamespacedKey getPaperKey() {
        return paperKey;
    }
}