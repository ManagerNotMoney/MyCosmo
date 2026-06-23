package io.github.managermoney;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Particle;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CashierListener implements Listener {

    private final Set<Location> registeredDrones = new HashSet<>();
    private final JavaPlugin plugin;
    private Economy economyCache;

    public CashierListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private Economy getEconomy() {
        if (economyCache == null) {
            RegisteredServiceProvider<Economy> rsp =
                    plugin.getServer().getServicesManager().getRegistration(Economy.class);
            economyCache = rsp != null ? rsp.getProvider() : null;
        }
        return economyCache;
    }

    private void spawnParticle(World world, int x, int y, int z) {
        world.spawnParticle(Particle.CLOUD, x + 0.5, y + 0.5, z + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
    }

    private boolean isCashierStructure(Block barrel) {
        if (barrel.getType() != Material.BARREL) return false;
        Location loc = barrel.getLocation();
        World w = barrel.getWorld();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();

        Block centerBar = w.getBlockAt(x, y + 1, z);
        if (centerBar.getType() != Material.IRON_BARS) return false;

        if (w.getBlockAt(x - 1, y + 2, z).getType() == Material.IRON_BARS &&
                w.getBlockAt(x, y + 2, z).getType() == Material.IRON_BARS &&
                w.getBlockAt(x + 1, y + 2, z).getType() == Material.IRON_BARS) return true;

        if (w.getBlockAt(x, y + 2, z - 1).getType() == Material.IRON_BARS &&
                w.getBlockAt(x, y + 2, z).getType() == Material.IRON_BARS &&
                w.getBlockAt(x, y + 2, z + 1).getType() == Material.IRON_BARS) return true;

        return false;
    }

    private void destroyStructure(Block barrel) {
        Location loc = barrel.getLocation();
        World w = barrel.getWorld();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();

        barrel.setType(Material.AIR);
        spawnParticle(w, x, y, z);
        w.getBlockAt(x, y + 1, z).setType(Material.AIR);
        spawnParticle(w, x, y + 1, z);

        if (w.getBlockAt(x - 1, y + 2, z).getType() == Material.IRON_BARS) {
            w.getBlockAt(x - 1, y + 2, z).setType(Material.AIR); spawnParticle(w, x - 1, y + 2, z);
            w.getBlockAt(x, y + 2, z).setType(Material.AIR);     spawnParticle(w, x, y + 2, z);
            w.getBlockAt(x + 1, y + 2, z).setType(Material.AIR); spawnParticle(w, x + 1, y + 2, z);
        } else if (w.getBlockAt(x, y + 2, z - 1).getType() == Material.IRON_BARS) {
            w.getBlockAt(x, y + 2, z - 1).setType(Material.AIR); spawnParticle(w, x, y + 2, z - 1);
            w.getBlockAt(x, y + 2, z).setType(Material.AIR);     spawnParticle(w, x, y + 2, z);
            w.getBlockAt(x, y + 2, z + 1).setType(Material.AIR); spawnParticle(w, x, y + 2, z + 1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (placed.getType() == Material.BARREL && isCashierStructure(placed)) {
            if (registeredDrones.add(placed.getLocation().clone())) {
                player.sendMessage(Lang.get("cashier.registered"));
            }
            return;
        }
        for (int dy = 1; dy <= 2; dy++) {
            Block candidateBarrel = placed.getWorld().getBlockAt(placed.getX(), placed.getY() - dy, placed.getZ());
            if (candidateBarrel.getType() == Material.BARREL && isCashierStructure(candidateBarrel)) {
                if (registeredDrones.add(candidateBarrel.getLocation().clone())) {
                    player.sendMessage(Lang.get("cashier.registered"));
                }
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block candidate = broken.getWorld().getBlockAt(
                            broken.getX() + dx, broken.getY() + dy, broken.getZ() + dz);
                    if (candidate.getType() == Material.BARREL &&
                            registeredDrones.contains(candidate.getLocation())) {
                        registeredDrones.remove(candidate.getLocation());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.BARREL) return;
        if (isCashierStructure(clicked)) {
            if (registeredDrones.add(clicked.getLocation().clone())) {
                event.getPlayer().sendMessage(Lang.get("cashier.registered"));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Barrel)) return;
        Barrel barrelState = (Barrel) event.getInventory().getHolder();
        if (barrelState == null) return;
        Block barrel = barrelState.getBlock();
        if (!isCashierStructure(barrel)) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        int totalPaid = 0;
        boolean found = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && DisketteManager.isDiskette(item) && DisketteManager.hasHash(item)) {
                int amount = item.getAmount();
                inv.clear(i);
                int valuePerItem = DisketteManager.getValue(item);
                if (valuePerItem > 0) totalPaid += amount * valuePerItem;
                found = true;
            }
        }

        if (found) {
            final Player finalPlayer = player;
            final int finalTotalPaid = totalPaid;
            final Block finalBarrel = barrel;

            player.sendMessage(Lang.get("cashier.sending"));
            destroyStructure(finalBarrel);
            registeredDrones.remove(finalBarrel.getLocation());

            int delayTicks = 200 + ThreadLocalRandom.current().nextInt(201);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finalPlayer.isOnline()) {
                    Economy econ = getEconomy();
                    if (econ != null) {
                        econ.depositPlayer(finalPlayer, finalTotalPaid);
                        finalPlayer.sendMessage(Lang.get("cashier.paid", "amount", finalTotalPaid));
                    } else {
                        finalPlayer.sendMessage(Lang.get("cashier.no_economy"));
                    }
                }
            }, delayTicks);
        }
    }
}
