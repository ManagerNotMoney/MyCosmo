package io.github.managermoney;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TelescopeGUI implements Listener {

    private static final String GUI_TITLE = "Радиотелескоп";
    private static final String LORE_COLOR = "§b";
    private static final long RECORD_DELAY = 228L;
    private static final long TUNING_DELAY = 200L;

    private static final int DISKETTE_SLOT = 0;
    private static final int ID_BELL_SLOT = 1;
    private static final int TUNING_SLOT = 2;
    private static final int DIRECTION_SLOT = 3;
    private static final int FREQUENCY_SLOT = 4;
    private static final int POLARIZATION_SLOT = 5;
    private static final int CALIBRATION_SLOT = 6;
    private static final int SCAN_BUTTON_SLOT = 7;
    private static final int RECORD_BUTTON_SLOT = 8;

    public static final List<Material> FREQUENCY_TORCHES = Arrays.asList(
            Material.SOUL_TORCH, Material.COPPER_TORCH, Material.TORCH, Material.REDSTONE_TORCH
    );
    public static final List<String> FREQUENCY_DESCRIPTIONS = Arrays.asList(
            "ДВ (150–450 кГц)", "СВ (500–1600 кГц)", "ВЧ (3–30 МГц)", "ОВЧ (30–300 МГц)"
    );
    public static final List<Material> DIRECTION_MATERIALS = Arrays.asList(
            Material.BLACK_CONCRETE, Material.BLUE_CONCRETE, Material.RED_CONCRETE, Material.WHITE_CONCRETE
    );
    public static final List<String> DIRECTION_NAMES = Arrays.asList(
            "Север", "Восток", "Юг", "Запад"
    );

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
    private final TelescopeListener telescopeListener;

    private final Map<Location, FrequencyDirection> settingsByLocation = new HashMap<>();
    private final Map<Location, CalibrationLevel> calibrationByLocation = new HashMap<>();
    private final Map<UUID, Location> openedTelescopes = new HashMap<>();
    private final Set<UUID> observatoryOpenedTelescopes = new HashSet<>();
    private final Map<Location, Set<Player>> locationViewers = new HashMap<>();
    private final Map<Location, BukkitTask> scanningProgressTasks = new HashMap<>();
    private final Map<Location, TuningTask> tuningTasks = new HashMap<>();
    private final Map<Location, RecordingTask> recordingTasks = new HashMap<>();
    private final Map<Location, ItemStack> completedDiskettes = new HashMap<>();

    // Подписка на уведомления: Location -> Set<UUID подписчиков>
    private final Map<Location, Set<UUID>> notificationSubscribers = new HashMap<>();

    public TelescopeGUI(JavaPlugin plugin, TelescopeListener telescopeListener) {
        this.plugin = plugin;
        this.telescopeListener = telescopeListener;
    }

    public void open(Player player, Location baseLocation) {
        open(player, baseLocation, false);
    }

    public void openFromObservatory(Player player, Location baseLocation) {
        open(player, baseLocation, true);
    }

    private void open(Player player, Location baseLocation, boolean fromObservatory) {
        detachPlayerFromCurrentTelescope(player);
        openedTelescopes.put(player.getUniqueId(), baseLocation);
        if (fromObservatory) {
            observatoryOpenedTelescopes.add(player.getUniqueId());
        } else {
            observatoryOpenedTelescopes.remove(player.getUniqueId());
        }
        locationViewers.computeIfAbsent(baseLocation, k -> new HashSet<>()).add(player);

        FrequencyDirection settings = settingsByLocation.getOrDefault(
                baseLocation,
                new FrequencyDirection(FREQUENCY_TORCHES.get(0), DIRECTION_MATERIALS.get(0), Polarization.VERTICAL)
        );
        CalibrationLevel calibration = getCalibration(baseLocation);

        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, FILLER_ITEM.clone());
        }

        inv.setItem(DISKETTE_SLOT, null);
        inv.setItem(ID_BELL_SLOT, createIdBell(telescopeListener.getTelescopeId(baseLocation)));
        inv.setItem(DIRECTION_SLOT, createDirectionItem(settings.getDirection()));
        inv.setItem(FREQUENCY_SLOT, createFrequencyItem(settings.getFrequency()));
        inv.setItem(POLARIZATION_SLOT, createPolarizationItem(settings.getPolarization()));
        inv.setItem(CALIBRATION_SLOT, createCalibrationItem(calibration));

        if (TelescopeListener.getActiveSignals().containsKey(baseLocation)) {
            FrequencyDirection signal = TelescopeListener.getActiveSignals().get(baseLocation);
            inv.setItem(SCAN_BUTTON_SLOT, createSignalFoundButton(signal.getFrequency(), signal.getDirection(), signal.getPolarization()));
        } else {
            inv.setItem(SCAN_BUTTON_SLOT, createScanButton());
        }

        ItemStack completed = completedDiskettes.get(baseLocation);
        if (completed != null) {
            inv.setItem(DISKETTE_SLOT, completed.clone());
        }
        RecordingTask recTask = recordingTasks.get(baseLocation);
        if (recTask != null) {
            inv.setItem(TUNING_SLOT,
                    createRecordingProgressItem(
                            (int) (recTask.maxTicks - recTask.remaining),
                            (int) recTask.maxTicks
                    ));
            inv.setItem(RECORD_BUTTON_SLOT, createRecordButtonDisabled());
        } else {
            TuningTask tuningTask = tuningTasks.get(baseLocation);
            if (tuningTask != null) {
                inv.setItem(TUNING_SLOT,
                        createTuningItem(
                                tuningTask.type,
                                (int) (tuningTask.maxTicks - tuningTask.remaining),
                                (int) tuningTask.maxTicks
                        ));
            } else {
                inv.setItem(TUNING_SLOT,
                        createTuningItem(TuningType.FREQUENCY, 0, 0));
            }
            inv.setItem(RECORD_BUTTON_SLOT, createRecordButton());
        }

        player.openInventory(inv);
    }

    public CalibrationLevel getCalibration(Location location) {
        return calibrationByLocation.getOrDefault(location, CalibrationLevel.NOT_CALIBRATED);
    }

    public int getAdjustedScanDelay(Location location, int baseDelay) {
        CalibrationLevel calibration = getCalibration(location);
        return Math.max(60, (int) Math.round(baseDelay * calibration.getScanDelayMultiplier()));
    }

    public void setCalibration(Location location, CalibrationLevel calibration) {
        if (calibration == null) {
            calibration = CalibrationLevel.NOT_CALIBRATED;
        }
        calibrationByLocation.put(location, calibration);
    }

    private void detachPlayerFromCurrentTelescope(Player player) {
        Location previousLocation = openedTelescopes.remove(player.getUniqueId());
        if (previousLocation == null) return;
        Set<Player> viewers = locationViewers.get(previousLocation);
        if (viewers != null) {
            viewers.remove(player);
            if (viewers.isEmpty()) {
                locationViewers.remove(previousLocation);
            }
        }
    }

    private ItemStack createScanButton() {
        ItemStack button = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cНачать сканирование");
            meta.setLore(Arrays.asList(
                    "§7Нажмите, чтобы начать поиск сигнала",
                    "§7(Должно быть чистое небо над телескопом)"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createSignalFoundButton(Material freq, Material dir, Polarization polarization) {
        ItemStack button = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aСигнал найден!");
            meta.setLore(Arrays.asList(
                    "§7Частота: " + TelescopeUtil.formatFrequency(freq),
                    "§7Направление: " + TelescopeUtil.formatDirection(dir),
                    "§7Полярность: " + polarization.getDisplayName(),
                    "§7Нажмите, чтобы начать новое сканирование"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createRecordButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aЗаписать сигнал");
            meta.setLore(Arrays.asList(
                    "§7Нажмите, чтобы начать запись",
                    "§7(Дискета должна быть у вас в руке или в инвентаре)"
            ));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createRecordButtonDisabled() {
        ItemStack button = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cЗапись уже идёт");
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createDirectionItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Направление");
            int index = DIRECTION_MATERIALS.indexOf(material);
            meta.setLore(Collections.singletonList(LORE_COLOR + "Направление:" + DIRECTION_NAMES.get(index)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFrequencyItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Частота Сигнала");
            int index = FREQUENCY_TORCHES.indexOf(material);
            meta.setLore(Collections.singletonList(LORE_COLOR + "Частота:" + FREQUENCY_DESCRIPTIONS.get(index)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPolarizationItem(Polarization polarization) {
        ItemStack item = new ItemStack(polarization.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Полярность");
            meta.setLore(Arrays.asList(
                    LORE_COLOR + "Полярность: " + polarization.getDisplayName(),
                    "§7Нажмите, чтобы изменить полярность"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCalibrationItem(CalibrationLevel calibration) {
        ItemStack item = new ItemStack(calibration.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Калибровка");
            meta.setLore(Arrays.asList(
                    LORE_COLOR + "Состояние: " + calibration.getDisplayName(),
                    "§7Нажмите, чтобы изменить калибровку",
                    "§7Плохая калибровка замедляет поиск сигнала",
                    "§7и ухудшает качество записи"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTuningItem(TuningType type, int progress, int max) {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§bНастройка");
        if (max <= 0) {
            // Idle-состояние: настройка не идёт
            meta.setLore(Arrays.asList(
                    "",
                    "§7Здесь отображается прогресс настройки",
                    "§7частоты, направления, полярности",
                    "§7и калибровки телескопа.",
                    ""
            ));
        } else {
            int percent = (int) ((progress * 100.0) / max);
            meta.setLore(Arrays.asList(
                    "",
                    "§7Настройка: §f" + type.getDisplay(),
                    "",
                    "§7Прогресс: §b" + percent + "%",
                    ""
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRecordingProgressItem(int currentTick, int maxTicks) {
        ItemStack pane = new ItemStack(Material.GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Запись");
            if (maxTicks > 0) {
                int percent = (int) (currentTick * 100.0 / maxTicks);
                meta.setLore(Collections.singletonList("§6Прогресс: " + percent + "%"));
            } else {
                meta.setLore(Collections.singletonList("§aГотово"));
            }
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createIdBell(Integer id) {
        ItemStack bell = new ItemStack(Material.BELL);
        ItemMeta meta = bell.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Номер Радиовышки");
            List<String> lore = new ArrayList<>();
            lore.add("§7ID: " + (id == null ? "нет" : id));
            lore.add("§7Нажмите, чтобы подписаться/отписаться");
            lore.add("§7от уведомлений об окончании настроек");
            meta.setLore(lore);
            bell.setItemMeta(meta);
        }
        return bell;
    }

    public void resetScanButton(Location baseLoc) {
        ItemStack scanButton = createScanButton();
        Set<Player> viewers = locationViewers.get(baseLoc);
        if (viewers == null) return;
        for (Player p : new HashSet<>(viewers)) {
            if (p == null || !p.isOnline()) continue;
            Inventory inv = p.getOpenInventory().getTopInventory();
            if (inv != null && GUI_TITLE.equals(p.getOpenInventory().getTitle())) {
                inv.setItem(SCAN_BUTTON_SLOT, scanButton.clone());
            }
        }
    }

    public void onSignalFound(Location baseLoc, Material freq, Material dir, Polarization polarization) {
        Set<Player> viewers = locationViewers.get(baseLoc);
        if (viewers == null) return;
        ItemStack scanButton = createSignalFoundButton(freq, dir, polarization);
        for (Player p : new HashSet<>(viewers)) {
            if (p == null || !p.isOnline()) continue;
            Inventory inv = p.getOpenInventory().getTopInventory();
            if (inv != null && GUI_TITLE.equals(p.getOpenInventory().getTitle())) {
                inv.setItem(SCAN_BUTTON_SLOT, scanButton.clone());
            }
        }
    }

    public void removeTelescopeSettings(Location location) {
        settingsByLocation.remove(location);
        calibrationByLocation.remove(location);
        notificationSubscribers.remove(location);

        TuningTask tuningTask = tuningTasks.remove(location);
        if (tuningTask != null) tuningTask.cancel();

        RecordingTask recordingTask = recordingTasks.remove(location);
        if (recordingTask != null) {
            recordingTask.cancel();
            ItemStack unfinished = recordingTask.getDiskette();
            if (unfinished != null) {
                World world = location.getWorld();
                if (world != null) world.dropItemNaturally(location, unfinished);
            }
        }

        completedDiskettes.remove(location);
        locationViewers.remove(location);

        BukkitTask progressTask = scanningProgressTasks.remove(location);
        if (progressTask != null) progressTask.cancel();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        Location baseLoc = openedTelescopes.get(player.getUniqueId());
        if (baseLoc == null) return;

        if (event.getClickedInventory() != topInv) {
            event.setCancelled(true);
            return;
        }

        if (recordingTasks.containsKey(baseLoc) && rawSlot != DISKETTE_SLOT) {
            event.setCancelled(true);
            player.sendMessage("§cИдёт запись сигнала. Дождитесь окончания.");
            return;
        }

        if (rawSlot == SCAN_BUTTON_SLOT) {
            event.setCancelled(true);
            startScanning(player, baseLoc);
            return;
        }

        if (rawSlot == RECORD_BUTTON_SLOT) {
            event.setCancelled(true);
            startRecording(player, baseLoc, topInv);
            return;
        }

        if (rawSlot == DIRECTION_SLOT) {
            event.setCancelled(true);
            cycleDirection(baseLoc, topInv);
            return;
        }

        if (rawSlot == FREQUENCY_SLOT) {
            event.setCancelled(true);
            cycleFrequency(baseLoc, topInv);
            return;
        }

        if (rawSlot == POLARIZATION_SLOT) {
            event.setCancelled(true);
            cyclePolarization(baseLoc, topInv);
            return;
        }
        if (rawSlot == CALIBRATION_SLOT) {
            event.setCancelled(true);
            if (observatoryOpenedTelescopes.contains(player.getUniqueId())) {
                player.sendMessage("§cКалибровка доступна только при прямом открытии радиотелескопа.");
                return;
            }
            if (tuningTasks.containsKey(baseLoc)) {
                player.sendMessage("§cТелескоп уже настраивается.");
                return;
            }
            CalibrationLevel current = calibrationByLocation.getOrDefault(
                    baseLoc,
                    CalibrationLevel.NOT_CALIBRATED
            );
            CalibrationLevel next = current.next();
            if (next == current) {
                player.sendMessage("§aКалибровка уже максимальная.");
                return;
            }
            calibrationByLocation.put(baseLoc, next);
            topInv.setItem(CALIBRATION_SLOT, createCalibrationItem(next));
            startTuning(baseLoc, TuningType.CALIBRATION);
            // Убрано сообщение в чат — теперь только подписчикам
            return;
        }
        if (rawSlot == TUNING_SLOT || rawSlot == ID_BELL_SLOT) {
            event.setCancelled(true);
            if (rawSlot == ID_BELL_SLOT) {
                toggleNotification(player, baseLoc);
            }
            return;
        }

        if (rawSlot == DISKETTE_SLOT) {
            if (completedDiskettes.containsKey(baseLoc)) {
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage("§cСначала освободите курсор.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory current = player.getOpenInventory().getTopInventory();
                    if (current != null && GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                        ItemStack slotItem = current.getItem(DISKETTE_SLOT);
                        if (slotItem == null || slotItem.getType() == Material.AIR) {
                            completedDiskettes.remove(baseLoc);
                        }
                    }
                });
                return;
            }
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    private void toggleNotification(Player player, Location baseLoc) {
        Set<UUID> subs = notificationSubscribers.computeIfAbsent(baseLoc, k -> new HashSet<>());
        UUID uuid = player.getUniqueId();
        Integer id = telescopeListener.getTelescopeId(baseLoc);
        String idStr = id != null ? String.valueOf(id) : "???";

        if (subs.contains(uuid)) {
            subs.remove(uuid);
            player.sendMessage("§7[§6" + idStr + "§7] §cУведомления отключены.");
        } else {
            subs.add(uuid);
            player.sendMessage("§7[§6" + idStr + "§7] §aУведомления включены! Вы будете получать сообщения о завершении настроек и появлении сигнала.");
        }

        if (subs.isEmpty()) {
            notificationSubscribers.remove(baseLoc);
        }
    }

    private void notifySubscribers(Location baseLoc, String message) {
        Set<UUID> subs = notificationSubscribers.get(baseLoc);
        if (subs == null || subs.isEmpty()) return;

        Integer id = telescopeListener.getTelescopeId(baseLoc);
        String prefix = "§7[§6" + (id != null ? id : "???") + "§7] ";

        for (UUID uuid : new HashSet<>(subs)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(prefix + message);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location baseLoc = openedTelescopes.remove(uuid);
        observatoryOpenedTelescopes.remove(uuid);
        if (baseLoc != null) {
            Set<Player> viewers = locationViewers.get(baseLoc);
            if (viewers != null) {
                viewers.remove(player);
                // Оставляем запись locationViewers пока идёт задача —
                // RecordingTask/TuningTask добавят игрока обратно при повторном открытии
                boolean taskRunning = recordingTasks.containsKey(baseLoc)
                        || tuningTasks.containsKey(baseLoc);
                if (viewers.isEmpty() && !taskRunning) {
                    locationViewers.remove(baseLoc);
                }
            }
            // Не трогаем completedDiskettes если запись ещё идёт —
            // задача завершится сама и положит дискету в слот
            if (!recordingTasks.containsKey(baseLoc)) {
                Inventory topInv = event.getView().getTopInventory();
                ItemStack diskSlot = topInv.getItem(DISKETTE_SLOT);
                if (diskSlot == null || diskSlot.getType() == Material.AIR) {
                    completedDiskettes.remove(baseLoc);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location baseLoc = openedTelescopes.remove(uuid);
        observatoryOpenedTelescopes.remove(uuid);
        if (baseLoc != null) {
            Set<Player> viewers = locationViewers.get(baseLoc);
            if (viewers != null) {
                viewers.remove(player);
                if (viewers.isEmpty()) {
                    locationViewers.remove(baseLoc);
                }
            }
        }
        for (Set<UUID> subs : notificationSubscribers.values()) {
            subs.remove(uuid);
        }
    }



    public Set<Player> getLocationViewers(Location loc) {
        return locationViewers.getOrDefault(loc, Collections.emptySet());
    }

    private void cycleDirection(Location baseLoc, Inventory topInv) {
        FrequencyDirection current = settingsByLocation.getOrDefault(
                baseLoc,
                new FrequencyDirection(FREQUENCY_TORCHES.get(0), DIRECTION_MATERIALS.get(0), Polarization.VERTICAL)
        );
        int index = DIRECTION_MATERIALS.indexOf(current.getDirection());
        Material newDirection = DIRECTION_MATERIALS.get((index + 1) % DIRECTION_MATERIALS.size());
        settingsByLocation.put(baseLoc, new FrequencyDirection(current.getFrequency(), newDirection, current.getPolarization()));
        topInv.setItem(DIRECTION_SLOT, createDirectionItem(newDirection));
        startTuning(baseLoc, TuningType.DIRECTION);
    }

    private void cycleFrequency(Location baseLoc, Inventory topInv) {
        FrequencyDirection current = settingsByLocation.getOrDefault(
                baseLoc,
                new FrequencyDirection(FREQUENCY_TORCHES.get(0), DIRECTION_MATERIALS.get(0), Polarization.VERTICAL)
        );
        int index = FREQUENCY_TORCHES.indexOf(current.getFrequency());
        Material newFrequency = FREQUENCY_TORCHES.get((index + 1) % FREQUENCY_TORCHES.size());
        settingsByLocation.put(baseLoc, new FrequencyDirection(newFrequency, current.getDirection(), current.getPolarization()));
        topInv.setItem(FREQUENCY_SLOT, createFrequencyItem(newFrequency));
        startTuning(baseLoc, TuningType.FREQUENCY);
    }

    private void cyclePolarization(Location baseLoc, Inventory topInv) {
        FrequencyDirection current = settingsByLocation.getOrDefault(
                baseLoc,
                new FrequencyDirection(FREQUENCY_TORCHES.get(0), DIRECTION_MATERIALS.get(0), Polarization.VERTICAL)
        );
        Polarization newPolarization = current.getPolarization().next();
        settingsByLocation.put(baseLoc, new FrequencyDirection(current.getFrequency(), current.getDirection(), newPolarization));
        topInv.setItem(POLARIZATION_SLOT, createPolarizationItem(newPolarization));
        startTuning(baseLoc, TuningType.POLARIZATION);
    }

    private void startTuning(Location loc, TuningType type) {
        TuningTask oldTask = tuningTasks.remove(loc);
        if (oldTask != null) oldTask.cancel();
        TuningTask task = new TuningTask(loc, TUNING_DELAY, type);
        tuningTasks.put(loc, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    private void startScanning(Player player, Location baseLoc) {
        if (tuningTasks.containsKey(baseLoc)) {
            player.sendMessage("§cТелескоп настраивается. Подождите окончания настройки.");
            return;
        }
        if (recordingTasks.containsKey(baseLoc)) {
            player.sendMessage("§cИдёт запись сигнала. Дождитесь окончания.");
            return;
        }
        if (telescopeListener.isScanning(baseLoc)) {
            player.sendMessage("§cСканирование уже выполняется.");
            return;
        }
        if (TelescopeListener.getActiveSignals().containsKey(baseLoc)) {
            player.sendMessage("§cУ телескопа уже есть активный сигнал.");
            return;
        }

        Block bottom = baseLoc.getBlock();
        Block topBlock = bottom.getWorld().getBlockAt(bottom.getX(), bottom.getY() + 4, bottom.getZ());
        if (!TelescopeUtil.isSkyClear(topBlock)) {
            player.sendMessage("§cОбзор закрыт. Сигнал не может быть получен.");
            return;
        }

        Material randomFreq = FREQUENCY_TORCHES.get(ThreadLocalRandom.current().nextInt(FREQUENCY_TORCHES.size()));
        Material randomDir = DIRECTION_MATERIALS.get(ThreadLocalRandom.current().nextInt(DIRECTION_MATERIALS.size()));
        Polarization randomPol = Polarization.random();
        FrequencyDirection signal = new FrequencyDirection(randomFreq, randomDir, randomPol);

        int baseDelay = 300 + ThreadLocalRandom.current().nextInt(501);
        int delay = getAdjustedScanDelay(baseLoc, baseDelay);

        World world = baseLoc.getWorld();
        if (world != null) {
            if (world.hasStorm() || world.isThundering()) {
                delay = (int) (delay * 1.2);
            }
            if (!world.isDayTime()) {
                delay = (int) (delay * 0.85);
            }
        }

        if (telescopeListener.hasNearbyTelescope(baseLoc, 20.0)) {
            delay = (int) (delay * 1.5);
        }

        // Убрано сообщение "Начинается сканирование..." — теперь только подписчикам
        player.playSound(baseLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        startScanningProgress(baseLoc, delay);

        BukkitTask scanTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask progressTask = scanningProgressTasks.remove(baseLoc);
            if (progressTask != null) progressTask.cancel();
            try {
                if (!telescopeListener.isTelescopeExists(baseLoc)) {
                    resetScanButton(baseLoc);
                    return;
                }
                if (TelescopeListener.getActiveSignals().containsKey(baseLoc)) {
                    resetScanButton(baseLoc);
                    return;
                }
                if (shouldMissSignal(baseLoc)) {
                    resetScanButton(baseLoc);
                    broadcastToViewers(baseLoc, "§cСканирование завершено. Сигнал не обнаружен.");
                    notifySubscribers(baseLoc, "§cСканирование завершено. Сигнал не обнаружен.");
                    return;
                }
                World signalWorld = baseLoc.getWorld();
                if (signalWorld != null && !signalWorld.isDayTime() && ThreadLocalRandom.current().nextInt(100) < 4) {
                    Set<Player> currentViewers = locationViewers.get(baseLoc);
                    if (currentViewers != null) {
                        for (Player p : new HashSet<>(currentViewers)) {
                            if (p != null && p.isOnline()) {
                                p.playSound(baseLoc, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                            }
                        }
                    }
                }
                TelescopeListener.getActiveSignals().put(baseLoc, signal);
                telescopeListener.startSignalExpiry(baseLoc);
                onSignalFound(baseLoc, randomFreq, randomDir, randomPol);
                notifySubscribers(baseLoc, "§aСигнал обнаружен! " + TelescopeUtil.formatFrequency(randomFreq) + ", " + TelescopeUtil.formatDirection(randomDir));
            } finally {
                telescopeListener.cancelScanning(baseLoc);
            }
        }, delay);

        telescopeListener.registerScanTask(baseLoc, scanTask);
    }

    public void startScanningProgress(Location baseLoc, int totalTicks) {
        BukkitRunnable runnable = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!telescopeListener.isTelescopeExists(baseLoc)) {
                    cancel();
                    scanningProgressTasks.remove(baseLoc);
                    return;
                }
                elapsed += 20;
                if (elapsed > totalTicks) elapsed = totalTicks;
                int percent = (int) ((double) elapsed / totalTicks * 100);
                updateScanButtonProgress(baseLoc, percent);
                if (elapsed >= totalTicks) {
                    cancel();
                    scanningProgressTasks.remove(baseLoc);
                }
            }
        };
        BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
        scanningProgressTasks.put(baseLoc, task);
    }

    public void stopScanningProgress(Location baseLoc) {
        BukkitTask task = scanningProgressTasks.remove(baseLoc);
        if (task != null) task.cancel();
    }

    public void broadcastToViewers(Location baseLoc, String message) {
        Set<Player> viewers = locationViewers.get(baseLoc);
        if (viewers == null) return;
        for (Player p : new HashSet<>(viewers)) {
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    private void updateScanButtonProgress(Location baseLoc, int percent) {
        Set<Player> viewers = locationViewers.get(baseLoc);
        if (viewers == null) return;
        ItemStack scanButton = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = scanButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eСканирование...");
            meta.setLore(Arrays.asList(
                    "§7Прогресс: " + percent + "%",
                    "§7Подождите, идёт поиск сигнала..."
            ));
            scanButton.setItemMeta(meta);
        }
        for (Player p : new HashSet<>(viewers)) {
            if (p == null || !p.isOnline()) continue;
            Inventory inv = p.getOpenInventory().getTopInventory();
            if (inv != null && GUI_TITLE.equals(p.getOpenInventory().getTitle())) {
                inv.setItem(SCAN_BUTTON_SLOT, scanButton.clone());
            }
        }
    }

    private boolean isSkyClear(Block startBlock) {
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

    private String determineRecordedQuality(CalibrationLevel calibration) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String terrible = DisketteManager.QUALITIES.get(0);
        String average  = DisketteManager.QUALITIES.get(2);
        switch (calibration) {
            case NOT_CALIBRATED:
            case TERRIBLE:
            case BAD:
                return terrible;
            case INACCURATE:
                return random.nextInt(100) < 70 ? terrible : average;
            case NORMAL:
                return random.nextInt(100) < 40 ? terrible : average;
            case EXCELLENT:
                return average;
            default:
                return terrible;
        }
    }

    private void startRecording(Player player, Location baseLoc, Inventory topInv) {
        if (tuningTasks.containsKey(baseLoc)) {
            player.sendMessage("§cТелескоп настраивается. Подождите окончания настройки.");
            return;
        }
        if (recordingTasks.containsKey(baseLoc)) {
            player.sendMessage("§cЗапись уже идёт на этом телескопе.");
            return;
        }
        FrequencyDirection signal = TelescopeListener.getActiveSignals().get(baseLoc);
        if (signal == null) {
            player.sendMessage("§cНет активного сигнала. Сначала нажмите ПКМ по красному блоку.");
            return;
        }
        if (telescopeListener.isScanning(baseLoc)) {
            player.sendMessage("§cСканирование ещё не завершено. Подождите.");
            return;
        }

        FrequencyDirection settings = settingsByLocation.getOrDefault(
                baseLoc,
                new FrequencyDirection(FREQUENCY_TORCHES.get(0), DIRECTION_MATERIALS.get(0), Polarization.VERTICAL)
        );
        if (!settings.getFrequency().equals(signal.getFrequency()) ||
                !settings.getDirection().equals(signal.getDirection()) ||
                settings.getPolarization() != signal.getPolarization()) {
            player.sendMessage("§cЧастота, направление и/или полярность не совпадают с параметрами сигнала!");
            return;
        }

        ItemStack diskette = null;
        int slot = -1;
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (DisketteManager.isDiskette(handItem) && !DisketteManager.hasHash(handItem)) {
            diskette = handItem.clone();
            diskette.setAmount(1);
            slot = -2;
        } else {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && DisketteManager.isDiskette(item) && !DisketteManager.hasHash(item)) {
                    diskette = item.clone();
                    diskette.setAmount(1);
                    slot = i;
                    break;
                }
            }
        }

        if (diskette == null) {
            player.sendMessage("§cУ вас нет пустой дискеты (без хэша) в инвентаре или в руке.");
            return;
        }

        if (slot == -2) {
            if (handItem.getAmount() > 1) {
                handItem.setAmount(handItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(handItem);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else if (slot >= 0) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(slot, item);
            } else {
                player.getInventory().clear(slot);
            }
        }

        String recordedQuality = determineRecordedQuality(getCalibration(baseLoc));
        World recWorld = baseLoc.getWorld();
        if (recWorld != null && (recWorld.hasStorm() || recWorld.isThundering())) {
            recordedQuality = degradeQualityByOne(recordedQuality);
        }
        RecordingTask task = new RecordingTask(baseLoc, diskette, RECORD_DELAY, recordedQuality, player);
        recordingTasks.put(baseLoc, task);
        task.runTaskTimer(plugin, 0L, 20L);

        topInv.setItem(RECORD_BUTTON_SLOT, createRecordButtonDisabled());
        topInv.setItem(TUNING_SLOT, createRecordingProgressItem(0, (int) RECORD_DELAY));
        // Убрано сообщение "Запись началась" — теперь только подписчикам
    }

    private class TuningTask extends BukkitRunnable {
        private final TuningType type;
        private final Location loc;
        private final long maxTicks;
        private long remaining;

        private TuningTask(Location loc, long maxTicks, TuningType type) {
            this.loc = loc;
            this.maxTicks = maxTicks;
            this.remaining = maxTicks;
            this.type = type;
        }

        @Override
        public void run() {
            Set<Player> viewers = locationViewers.get(loc);
            if (viewers != null) {
                for (Player player : new HashSet<>(viewers)) {
                    if (player == null || !player.isOnline()) continue;
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    if (topInv != null && GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                        topInv.setItem(
                                TUNING_SLOT,
                                createTuningItem(
                                        type,
                                        (int) (maxTicks - remaining),
                                        (int) maxTicks
                                )
                        );
                    }
                }
            }
            remaining -= 20;
            if (remaining <= 0) {
                if (viewers != null) {
                    for (Player player : new HashSet<>(viewers)) {
                        if (player == null || !player.isOnline()) continue;
                        Inventory topInv = player.getOpenInventory().getTopInventory();
                        if (topInv != null && GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                            topInv.setItem(TUNING_SLOT, createTuningItem(type, 0, 0));
                        }
                    }
                }
                tuningTasks.remove(loc);
                notifySubscribers(loc, "§aНастройка §f" + type.getDisplay() + " §aзавершена!");
                cancel();
            }
        }
    }

    private class RecordingTask extends BukkitRunnable {
        private final Location loc;
        private final long maxTicks;
        private long remaining;
        private final ItemStack diskette;
        private String recordedQuality;
        private final Player initiator;

        private RecordingTask(Location loc, ItemStack diskette, long maxTicks, String recordedQuality, Player initiator) {
            this.loc = loc;
            this.diskette = diskette;
            this.maxTicks = maxTicks;
            this.remaining = maxTicks;
            this.recordedQuality = recordedQuality;
            this.initiator = initiator;
        }

        public ItemStack getDiskette() {
            return diskette;
        }

        @Override
        public void run() {
            Set<Player> viewers = locationViewers.get(loc);
            if (viewers != null) {
                for (Player player : new HashSet<>(viewers)) {
                    if (player == null || !player.isOnline()) continue;
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    if (topInv != null && GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                        topInv.setItem(TUNING_SLOT, createRecordingProgressItem((int) (maxTicks - remaining), (int) maxTicks));
                    }
                }
            }

            remaining -= 20;
            if (remaining <= 0) {
                try {
                    String hash = generateRandomHash();
                    SignalSourceManager.SignalSource source = SignalSourceManager.random();
                    String sourceName = source.getDisplayName();
                    String message = source.randomMessage();

                    boolean isEncryptedSource = "transmitter".equalsIgnoreCase(source.getId())
                            || "ottoman_relay".equalsIgnoreCase(source.getId());
                    if (isEncryptedSource) {
                        recordedQuality = DisketteManager.QUALITIES.get(0);
                        DisketteManager.setEncrypted(diskette, true);
                    } else {
                        DisketteManager.clearEncrypted(diskette);
                    }

                    DisketteManager.setHash(diskette, hash);
                    DisketteManager.setQuality(diskette, recordedQuality);
                    DisketteManager.setSource(diskette, sourceName);
                    DisketteManager.setMessage(diskette, message);

                    completedDiskettes.put(loc, diskette.clone());
                    degradeCalibration(loc);
                    TelescopeListener.getActiveSignals().remove(loc);
                    resetScanButton(loc);
                    telescopeListener.cancelSignalExpiry(loc);

                    if (viewers != null) {
                        for (Player player : new HashSet<>(viewers)) {
                            if (player == null || !player.isOnline()) continue;
                            Inventory topInv = player.getOpenInventory().getTopInventory();
                            if (topInv != null && GUI_TITLE.equals(player.getOpenInventory().getTitle())) {
                                topInv.setItem(DISKETTE_SLOT, diskette.clone());
                                topInv.setItem(TUNING_SLOT, createTuningItem(TuningType.FREQUENCY, 0, 0));
                                topInv.setItem(RECORD_BUTTON_SLOT, createRecordButton());
                                player.updateInventory();
                            }
                        }
                    }

                    // Убрано сообщение initiator — теперь только подписчикам
                    loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.0f);
                    notifySubscribers(loc, "§aЗапись сигнала завершена! Дискета готова.");
                } catch (Exception ex) {
                    plugin.getLogger().warning("[TelescopeGUI] Ошибка завершения записи: " + ex.getMessage());
                } finally {
                    recordingTasks.remove(loc);
                    cancel();
                }
            }
        }
    }

    private void degradeCalibration(Location loc) {
        CalibrationLevel current = calibrationByLocation.getOrDefault(
                loc,
                CalibrationLevel.NOT_CALIBRATED
        );
        int degrade = 1 + ThreadLocalRandom.current().nextInt(2);
        int newOrdinal = current.ordinal() - degrade;
        if (newOrdinal < 0) newOrdinal = 0;
        CalibrationLevel degraded = CalibrationLevel.values()[newOrdinal];
        calibrationByLocation.put(loc, degraded);
        Set<Player> viewers = locationViewers.get(loc);
        if (viewers != null) {
            for (Player p : viewers) {
                Inventory inv = p.getOpenInventory().getTopInventory();
                if (inv != null && GUI_TITLE.equals(p.getOpenInventory().getTitle())) {
                    inv.setItem(CALIBRATION_SLOT, createCalibrationItem(degraded));
                    p.updateInventory();
                }
            }
        }
    }

    private String degradeQualityByOne(String quality) {
        int index = DisketteManager.QUALITIES.indexOf(quality);
        if (index > 0) {
            return DisketteManager.QUALITIES.get(index - 1);
        }
        return quality;
    }

    public boolean shouldMissSignal(Location baseLoc) {
        CalibrationLevel cal = getCalibration(baseLoc);
        int chance;
        switch (cal) {
            case NOT_CALIBRATED: chance = 50; break;
            case TERRIBLE: chance = 40; break;
            case BAD: chance = 30; break;
            case INACCURATE: chance = 20; break;
            case NORMAL: chance = 15; break;
            case EXCELLENT: chance = 10; break;
            default: chance = 50;
        }
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    private String generateRandomHash() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}