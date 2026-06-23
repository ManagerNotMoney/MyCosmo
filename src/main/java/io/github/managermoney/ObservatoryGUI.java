package io.github.managermoney;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ObservatoryGUI implements Listener {

    private static final int TELESCOPE_SLOT_A = 3;
    private static final int TELESCOPE_SLOT_B = 4;
    private static final int TELESCOPE_SLOT_C = 5;

    private static final ItemStack FILLER_ITEM;
    static {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
        FILLER_ITEM = filler;
    }

    private final JavaPlugin plugin;
    private final TelescopeGUI telescopeGUI;
    private final TelescopeListener telescopeListener;
    private ObservatoryListener observatoryListener;

    private final Map<UUID, Block> openedObservatories = new HashMap<>();

    public ObservatoryGUI(JavaPlugin plugin, TelescopeGUI telescopeGUI, TelescopeListener telescopeListener) {
        this.plugin            = plugin;
        this.telescopeGUI      = telescopeGUI;
        this.telescopeListener = telescopeListener;
    }

    public void setObservatoryListener(ObservatoryListener listener) {
        this.observatoryListener = listener;
    }

    private String guiTitle() {
        return Lang.get("observatory.gui_title");
    }

    public void open(Player player, Block observatoryBase) {
        openedObservatories.put(player.getUniqueId(), observatoryBase);

        Inventory inv = Bukkit.createInventory(null, 9, guiTitle());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, FILLER_ITEM.clone());

        if (observatoryListener != null) {
            ItemStack[] saved = observatoryListener.getSavedContents(observatoryBase);
            inv.setItem(TELESCOPE_SLOT_A, saved != null && saved.length > 0 ? saved[0] : null);
            inv.setItem(TELESCOPE_SLOT_B, saved != null && saved.length > 1 ? saved[1] : null);
            inv.setItem(TELESCOPE_SLOT_C, saved != null && saved.length > 2 ? saved[2] : null);
        } else {
            inv.setItem(TELESCOPE_SLOT_A, null);
            inv.setItem(TELESCOPE_SLOT_B, null);
            inv.setItem(TELESCOPE_SLOT_C, null);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!guiTitle().equals(event.getView().getTitle())) return;

        Player player    = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();
        Block obsBase    = openedObservatories.get(player.getUniqueId());
        if (obsBase == null) return;

        if (event.getClickedInventory() != topInv) {
            if (event.getClick() == ClickType.SHIFT_LEFT ||
                    event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getRawSlot();
        if (slot == TELESCOPE_SLOT_A || slot == TELESCOPE_SLOT_B || slot == TELESCOPE_SLOT_C) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.PAPER) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    NamespacedKey key = telescopeListener.getPaperKey();
                    Integer id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                    if (id != null && (event.getClick() == ClickType.RIGHT ||
                            event.getClick() == ClickType.SHIFT_RIGHT)) {
                        Location loc = telescopeListener.getLocationByTelescopeId(id);
                        if (loc != null) {
                            player.closeInventory();
                            telescopeGUI.openFromObservatory(player, loc);
                        } else {
                            player.sendMessage(Lang.get("telescope.not_found_id"));
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!guiTitle().equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        Block obsBase = openedObservatories.remove(player.getUniqueId());
        if (obsBase == null || observatoryListener == null) return;

        Inventory topInv = event.getView().getTopInventory();
        ItemStack slotA = topInv.getItem(TELESCOPE_SLOT_A);
        ItemStack slotB = topInv.getItem(TELESCOPE_SLOT_B);
        ItemStack slotC = topInv.getItem(TELESCOPE_SLOT_C);
        observatoryListener.setSavedContents(obsBase, new ItemStack[]{
                slotA == null ? null : slotA.clone(),
                slotB == null ? null : slotB.clone(),
                slotC == null ? null : slotC.clone()
        });
        observatoryListener.saveObservatories();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openedObservatories.remove(event.getPlayer().getUniqueId());
    }
}
