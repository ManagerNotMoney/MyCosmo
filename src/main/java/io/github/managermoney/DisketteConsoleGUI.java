package io.github.managermoney;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DisketteConsoleGUI implements Listener {

    private static final String GUI_TITLE  = "Дискетная Консоль";
    private static final int DISKETTE_SLOT = 0;
    private static final int BELL_SLOT = 1;
    private static final int PROGRESS_SLOT = 4;
    private static final int PREV_SLOT     = 6;
    private static final int NEXT_SLOT     = 7;
    private static final int MODE_SLOT     = 8;
    private static final long UPGRADE_DELAY = 300L;

    private static final ItemStack FILLER_ITEM;

    static {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        FILLER_ITEM = filler;
    }

    private final JavaPlugin plugin;
    private DisketteConsoleListener consoleListener;

    private final Map<UUID, Block>        openedConsoles = new HashMap<>();
    private final Map<Block, UpgradeMode> selectedModes  = new HashMap<>();
    private final Map<Block, UpgradeTask> tasks          = new HashMap<>();
    private final Map<Block, ItemStack> storedDiskettes   = new HashMap<>();

    // Подписка на уведомления: Block -> Set<UUID подписчиков>
    private final Map<Block, Set<UUID>> notificationSubscribers = new HashMap<>();

    public DisketteConsoleGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setConsoleListener(DisketteConsoleListener listener) {
        this.consoleListener = listener;
    }

    public void open(Player player, Block consoleBase) {
        openedConsoles.put(player.getUniqueId(), consoleBase);

        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER_ITEM.clone());

        ItemStack storedDiskette = storedDiskettes.get(consoleBase);
        inv.setItem(DISKETTE_SLOT, storedDiskette == null ? null : storedDiskette.clone());

        UpgradeTask currentTask = tasks.get(consoleBase);
        if (currentTask != null) {
            inv.setItem(PROGRESS_SLOT, createProgressItem(
                    (int) (currentTask.maxTicks - currentTask.remaining),
                    (int) currentTask.maxTicks
            ));
        } else {
            inv.setItem(PROGRESS_SLOT, createProgressItem(0, 0));
        }

        UpgradeMode mode = consoleListener != null
                ? consoleListener.getMode(consoleBase)
                : UpgradeMode.NONE;
        selectedModes.put(consoleBase, mode);
        inv.setItem(MODE_SLOT, createModeItem(mode));
        inv.setItem(PREV_SLOT, createArrow("◀"));
        inv.setItem(NEXT_SLOT, createArrow("▶"));
        inv.setItem(BELL_SLOT, createBellItem(consoleBase, player.getUniqueId()));

        player.openInventory(inv);
    }

    private ItemStack createBellItem(Block consoleBase, UUID playerUuid) {
        ItemStack bell = new ItemStack(Material.BELL);
        ItemMeta meta = bell.getItemMeta();
        if (meta != null) {
            Set<UUID> subs = notificationSubscribers.get(consoleBase);
            boolean isSubscribed = subs != null && subs.contains(playerUuid);
            meta.setDisplayName("§6Уведомления");
            List<String> lore = new ArrayList<>();
            lore.add(isSubscribed ? "§aПодписка активна" : "§cПодписка неактивна");
            lore.add("§7Нажмите, чтобы подписаться/отписаться");
            lore.add("§7от уведомлений об улучшении дискеты");
            meta.setLore(lore);
            bell.setItemMeta(meta);
        }
        return bell;
    }

    private ItemStack createModeItem(UpgradeMode mode) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eРежим улучшения");
            meta.setLore(Arrays.asList(
                    mode.getDisplayName(),
                    "§7Выберите режим, соответствующий",
                    "§7текущему качеству дискеты.",
                    "§7Для " + qualityHint(mode)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrow(String symbol) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7" + symbol);
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    private ItemStack createProgressItem(int current, int max) {
        ItemStack pane = new ItemStack(Material.GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fПрогресс");
            String lore = (max > 0)
                    ? "§6" + (int)(current * 100.0 / max) + "%"
                    : "§7Ожидание...";
            meta.setLore(Collections.singletonList(lore));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private String qualityHint(UpgradeMode mode) {
        switch (mode) {
            case DECODER:       return "§cЗашифровано → Ужасное";
            case DENOISE:       return "§aУжасное → Плохое";
            case EQUALIZE:      return "§bПлохое → Среднее";
            case DYNAMIC_RANGE: return "§6Среднее → Хорошее";
            case MASTERING:     return "§dХорошее → Отличное";
            default:            return "§cнет режима → улучшение невозможно";
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();
        Block consoleBase = openedConsoles.get(player.getUniqueId());
        if (consoleBase == null) return;

        if (event.getClickedInventory() != topInv) {
            if (event.getClick() == ClickType.SHIFT_LEFT ||
                    event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getRawSlot();

        if (slot == PREV_SLOT || slot == NEXT_SLOT) {
            event.setCancelled(true);
            UpgradeMode cur = selectedModes.getOrDefault(consoleBase, UpgradeMode.NONE);
            UpgradeMode next = (slot == NEXT_SLOT) ? cur.next() : cur.previous();
            selectedModes.put(consoleBase, next);
            topInv.setItem(MODE_SLOT, createModeItem(next));
            if (consoleListener != null) {
                consoleListener.setMode(consoleBase, next);
                consoleListener.saveConsoles();
            }
            ItemStack diskette = topInv.getItem(DISKETTE_SLOT);
            if (diskette != null && DisketteManager.isDiskette(diskette) && DisketteManager.hasHash(diskette)) {
                String q = DisketteManager.getQuality(diskette);
                if (next != UpgradeMode.getRequiredMode(q)) {
                    cancelTask(consoleBase);
                    topInv.setItem(PROGRESS_SLOT, createProgressItem(0, 0));
                }
            }
            return;
        }

        if (slot == PROGRESS_SLOT) {
            event.setCancelled(true);
            return;
        }

        if (slot == BELL_SLOT) {
            event.setCancelled(true);
            toggleNotification(player, consoleBase);
            topInv.setItem(BELL_SLOT, createBellItem(consoleBase, player.getUniqueId()));
            return;
        }

        if (slot == DISKETTE_SLOT) {
            scheduleUpgradeCheck(player, topInv, consoleBase);
            return;
        }

        event.setCancelled(true);
    }

    private void toggleNotification(Player player, Block consoleBase) {
        Set<UUID> subs = notificationSubscribers.computeIfAbsent(consoleBase, k -> new HashSet<>());
        UUID uuid = player.getUniqueId();

        if (subs.contains(uuid)) {
            subs.remove(uuid);
            player.sendMessage("§cУведомления об улучшении дискеты отключены.");
        } else {
            subs.add(uuid);
            player.sendMessage("§aУведомления об улучшении дискеты включены!");
        }

        if (subs.isEmpty()) {
            notificationSubscribers.remove(consoleBase);
        }
    }

    private void notifySubscribers(Block consoleBase, String message) {
        Set<UUID> subs = notificationSubscribers.get(consoleBase);
        if (subs == null || subs.isEmpty()) return;

        for (UUID uuid : new HashSet<>(subs)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§7[§bДискетная Консоль§7] " + message);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        Block consoleBase = openedConsoles.remove(player.getUniqueId());
        if (consoleBase == null) return;

        Inventory topInv = event.getView().getTopInventory();
        ItemStack diskette = topInv.getItem(DISKETTE_SLOT);
        if (diskette == null || diskette.getType() == Material.AIR) {
            storedDiskettes.remove(consoleBase);
        } else {
            storedDiskettes.put(consoleBase, diskette.clone());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        openedConsoles.remove(uuid);
        for (Set<UUID> subs : notificationSubscribers.values()) {
            subs.remove(uuid);
        }
    }

    private void cancelTask(Block consoleBase) {
        UpgradeTask old = tasks.remove(consoleBase);
        if (old != null) old.cancel();
    }

    private void scheduleUpgradeCheck(Player player, Inventory topInv, Block consoleBase) {
        cancelTask(consoleBase);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = topInv.getItem(DISKETTE_SLOT);
            if (current == null || current.getType() == Material.AIR) {
                storedDiskettes.remove(consoleBase);
            } else {
                storedDiskettes.put(consoleBase, current.clone());
            }

            ItemStack diskette = storedDiskettes.get(consoleBase);
            if (diskette == null || !DisketteManager.isDiskette(diskette) || !DisketteManager.hasHash(diskette)) {
                updateProgressForConsole(consoleBase, 0, 0);
                return;
            }
            boolean encrypted = DisketteManager.isEncrypted(diskette);
            UpgradeMode selected = selectedModes.getOrDefault(consoleBase, UpgradeMode.NONE);

            if (encrypted) {
                if (selected != UpgradeMode.DECODER) {
                    if (player.isOnline()) {
                        player.sendMessage("§cДискета зашифрована! Сначала используйте режим §cДекодер§c.");
                    }
                    updateProgressForConsole(consoleBase, 0, 0);
                    return;
                }
                UpgradeTask task = new UpgradeTask(player, consoleBase, UPGRADE_DELAY);
                tasks.put(consoleBase, task);
                task.runTaskTimer(plugin, 0L, 20L);
                return;
            }

            String quality = DisketteManager.getQuality(diskette);
            if (quality == null || quality.equals("Отличное")) {
                updateProgressForConsole(consoleBase, 0, 0);
                return;
            }
            UpgradeMode required = UpgradeMode.getRequiredMode(quality);
            if (selected != required) {
                if (player.isOnline()) {
                    player.sendMessage("§cНеверный режим улучшения! Нужен: " + required.getDisplayName());
                }
                updateProgressForConsole(consoleBase, 0, 0);
                return;
            }
            UpgradeTask task = new UpgradeTask(player, consoleBase, UPGRADE_DELAY);
            tasks.put(consoleBase, task);
            task.runTaskTimer(plugin, 0L, 20L);
        }, 1L);
    }

    private void updateProgressForConsole(Block consoleBase, int current, int max) {
        for (Map.Entry<UUID, Block> entry : new HashMap<>(openedConsoles).entrySet()) {
            if (!consoleBase.equals(entry.getValue())) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;
            if (!GUI_TITLE.equals(p.getOpenInventory().getTitle())) continue;
            Inventory inv = p.getOpenInventory().getTopInventory();
            if (inv != null) {
                inv.setItem(PROGRESS_SLOT, createProgressItem(current, max));
            }
        }
    }

    private void updateDisketteForConsole(Block consoleBase, ItemStack diskette) {
        storedDiskettes.put(consoleBase, diskette.clone());
        for (Map.Entry<UUID, Block> entry : new HashMap<>(openedConsoles).entrySet()) {
            if (!consoleBase.equals(entry.getValue())) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;
            if (!GUI_TITLE.equals(p.getOpenInventory().getTitle())) continue;
            Inventory inv = p.getOpenInventory().getTopInventory();
            if (inv != null) {
                inv.setItem(DISKETTE_SLOT, diskette.clone());
            }
        }
    }

    public void clearStoredDiskette(Block consoleBase) {
        storedDiskettes.remove(consoleBase);
    }

    private class UpgradeTask extends BukkitRunnable {
        private final Player player;
        private final Block consoleBase;
        private final long maxTicks;
        private long remaining;

        UpgradeTask(Player player, Block consoleBase, long maxTicks) {
            this.player = player;
            this.consoleBase = consoleBase;
            this.maxTicks = maxTicks;
            this.remaining = maxTicks;
        }

        @Override
        public void run() {
            ItemStack disk = storedDiskettes.get(consoleBase);
            if (disk == null || !DisketteManager.isDiskette(disk) || !DisketteManager.hasHash(disk)) {
                updateProgressForConsole(consoleBase, 0, 0);
                finish();
                return;
            }
            String quality = DisketteManager.getQuality(disk);
            if (quality == null || quality.equals("Отличное")) {
                updateProgressForConsole(consoleBase, 0, 0);
                finish();
                return;
            }

            remaining -= 20;
            if (remaining <= 0) {
                if (DisketteManager.isEncrypted(disk)) {
                    DisketteManager.clearEncrypted(disk);
                    DisketteManager.setQuality(disk, "Ужасное");
                    updateDisketteForConsole(consoleBase, disk);
                    player.playSound(consoleBase.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.0f);
                    notifySubscribers(consoleBase, "§aДискета декодирована! Качество: §eУжасное");
                } else if (DisketteManager.upgradeQuality(disk)) {
                    updateDisketteForConsole(consoleBase, disk);
                    // Убрано сообщение player.sendMessage — теперь только подписчикам
                    player.playSound(consoleBase.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.0f);
                    notifySubscribers(consoleBase, "§aДискета улучшена до качества: §e" + DisketteManager.getQuality(disk));
                }
                updateProgressForConsole(consoleBase, 0, 0);
                finish();
                return;
            }

            updateProgressForConsole(consoleBase, (int) (maxTicks - remaining), (int) maxTicks);
        }

        private void finish() {
            cancel();
            tasks.remove(consoleBase);
        }
    }
}