package io.github.managermoney;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DisketteListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (!DisketteManager.isDiskette(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() == null) return;
            Material type = event.getClickedBlock().getType();
            if (type == Material.BLUE_GLAZED_TERRACOTTA ||
                    type == Material.REDSTONE_BLOCK ||
                    type == Material.LIGHT_BLUE_GLAZED_TERRACOTTA ||
                    type == Material.BARREL) {
                return;
            }
        }

        String source = DisketteManager.getSource(item);
        if (source == null) {
            player.sendMessage(Lang.get("diskette.no_signal"));
            return;
        }
        String message = DisketteManager.getMessage(item);
        if (message == null) message = "Неизвестный сигнал.";

        player.sendMessage(Lang.get("diskette.signal_from", "source", source));
        String quality = DisketteManager.getQuality(item);
        // "Отличное" / "Excellent" — без искажений
        if (quality != null && !quality.equals(DisketteManager.QUALITIES.get(4))) {
            message = DisketteManager.applyDistortion(message, quality);
        }
        player.sendMessage(Lang.get("diskette.message_label", "message", message));
    }
}
