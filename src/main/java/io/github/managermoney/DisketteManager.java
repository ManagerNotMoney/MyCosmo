package io.github.managermoney;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class DisketteManager {

    private static final String DISKETTE_KEY = "diskette";
    public static NamespacedKey qualityKey;
    private static NamespacedKey disketteKey;
    public static NamespacedKey sourceKey;
    public static NamespacedKey messageKey;
    public static NamespacedKey encryptedKey;

    /**
     * Качества дискет — теперь берутся из Lang, чтобы совпадать с локализацией.
     * Порядок важен: от худшего к лучшему.
     */
    public static List<String> QUALITIES;

    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]");

    public static void init(JavaPlugin plugin) {
        disketteKey  = new NamespacedKey(plugin, DISKETTE_KEY);
        qualityKey   = new NamespacedKey(plugin, "diskette_quality");
        sourceKey    = new NamespacedKey(plugin, "diskette_source");
        messageKey   = new NamespacedKey(plugin, "diskette_message");
        encryptedKey = new NamespacedKey(plugin, "diskette_encrypted");

        // Загружаем строки качества из локализации
        QUALITIES = Arrays.asList(
                Lang.get("diskette_quality.terrible"),
                Lang.get("diskette_quality.bad"),
                Lang.get("diskette_quality.average"),
                Lang.get("diskette_quality.good"),
                Lang.get("diskette_quality.excellent")
        );

        registerRecipe(plugin);
    }

    private static void setLoreLine(ItemStack item, String targetPrefix, String newLine,
                                    String insertAfterPrefix, int fallbackPos) {
        if (!isDiskette(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        int index = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (COLOR_PATTERN.matcher(lore.get(i)).replaceAll("").startsWith(targetPrefix)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            lore.set(index, newLine);
        } else {
            int insertPos = fallbackPos;
            if (insertAfterPrefix != null) {
                for (int i = 0; i < lore.size(); i++) {
                    if (COLOR_PATTERN.matcher(lore.get(i)).replaceAll("").startsWith(insertAfterPrefix)) {
                        insertPos = i + 1;
                        break;
                    }
                }
            }
            lore.add(Math.min(insertPos, lore.size()), newLine);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static void setHash(ItemStack item, String hash) {
        setLoreLine(item, "Хэш:[", "§aХэш:[" + hash + "]", null, 1);
    }

    public static void setSource(ItemStack item, String source) {
        setLoreLine(item, "Сигнал с:", "§aСигнал с: " + source, "Хэш:[", Integer.MAX_VALUE);
    }

    public static String getSource(ItemStack item) {
        if (!isDiskette(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String source = meta.getPersistentDataContainer().get(sourceKey, PersistentDataType.STRING);
        if (source != null) return source;

        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                String plain = COLOR_PATTERN.matcher(line).replaceAll("");
                if (plain.startsWith("Сигнал с:")) {
                    return plain.substring("Сигнал с:".length()).trim();
                }
            }
        }
        return null;
    }

    public static void setMessage(ItemStack item, String message) {
        if (!isDiskette(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(messageKey, PersistentDataType.STRING, message);
        item.setItemMeta(meta);
    }

    public static void setEncrypted(ItemStack item, boolean encrypted) {
        if (!isDiskette(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(encryptedKey, PersistentDataType.BOOLEAN, encrypted);
            item.setItemMeta(meta);
        }
    }

    public static boolean isEncrypted(ItemStack item) {
        if (!isDiskette(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Boolean val = meta.getPersistentDataContainer().get(encryptedKey, PersistentDataType.BOOLEAN);
        return val != null && val;
    }

    public static void clearEncrypted(ItemStack item) {
        if (!isDiskette(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(encryptedKey);
            item.setItemMeta(meta);
        }
    }

    public static ItemStack createDiskette() {
        ItemStack diskette = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = diskette.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.get("diskette.name"));
            meta.setLore(Arrays.asList(
                    Lang.get("diskette.description"),
                    Lang.get("diskette.hash_null")
            ));
            meta.getPersistentDataContainer().set(disketteKey, PersistentDataType.BOOLEAN, true);
            diskette.setItemMeta(meta);
        }
        return diskette;
    }

    public static boolean isDiskette(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_INGOT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(disketteKey, PersistentDataType.BOOLEAN);
    }

    public static int getValue(ItemStack item) {
        if (!isDiskette(item) || !hasHash(item)) return 0;
        String quality = getQuality(item);
        if (quality == null) return 0;

        int base;
        if (quality.equals(QUALITIES.get(0)))      base = 1;
        else if (quality.equals(QUALITIES.get(1))) base = 2;
        else if (quality.equals(QUALITIES.get(2))) base = 5;
        else if (quality.equals(QUALITIES.get(3))) base = 7;
        else if (quality.equals(QUALITIES.get(4))) base = 12;
        else return 0;

        String sourceName = getSource(item);
        if (sourceName != null) {
            SignalSourceManager.SignalSource src = SignalSourceManager.getByDisplayName(sourceName);
            if (src != null) base += src.getValueBonus();
        }
        return base;
    }

    public static String getQuality(ItemStack item) {
        if (!isDiskette(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String quality = meta.getPersistentDataContainer().get(qualityKey, PersistentDataType.STRING);
        if (quality != null) return quality;

        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                if (line.contains("Качество: ") || line.contains("Quality: ")) {
                    String[] parts = line.split(": ", 2);
                    if (parts.length > 1) {
                        String q = COLOR_PATTERN.matcher(parts[1].trim()).replaceAll("");
                        if (QUALITIES.contains(q)) {
                            meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, q);
                            item.setItemMeta(meta);
                            return q;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String applyDistortion(String message, String quality) {
        if (message == null || quality == null) return message;
        int index = QUALITIES.indexOf(quality);
        if (index < 0) return message;

        double charReplaceChance  = 0.0;
        double garbageInsertChance = 0.0;
        double obfuscateChance    = 0.0;
        int    maxGarbageLength   = 0;

        // index 0 = terrible, 1 = bad, 2 = average, 3 = good, 4 = excellent
        switch (index) {
            case 0: // terrible
                charReplaceChance   = 0.7;
                garbageInsertChance = 0.3;
                obfuscateChance     = 0.25;
                maxGarbageLength    = 8;
                break;
            case 1: // bad
                charReplaceChance   = 0.4;
                garbageInsertChance = 0.15;
                obfuscateChance     = 0.1;
                maxGarbageLength    = 5;
                break;
            case 2: // average
                charReplaceChance   = 0.15;
                garbageInsertChance = 0.05;
                obfuscateChance     = 0.03;
                maxGarbageLength    = 3;
                break;
            case 3: // good
                charReplaceChance   = 0.03;
                garbageInsertChance = 0.01;
                obfuscateChance     = 0.0;
                maxGarbageLength    = 2;
                break;
            case 4: // excellent
                return message;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(message);
        for (int i = 0; i < sb.length(); i++) {
            if (random.nextDouble() < charReplaceChance) {
                if (random.nextBoolean()) sb.setCharAt(i, (char) ('A' + random.nextInt(26)));
                else                      sb.setCharAt(i, (char) ('0' + random.nextInt(10)));
            }
        }
        for (int i = 0; i < sb.length(); i++) {
            if (random.nextDouble() < garbageInsertChance) {
                int garbageLen = 1 + random.nextInt(maxGarbageLength);
                StringBuilder garbage = new StringBuilder();
                for (int j = 0; j < garbageLen; j++) {
                    garbage.append((char) (random.nextBoolean()
                            ? 'A' + random.nextInt(26)
                            : '0' + random.nextInt(10)));
                }
                sb.insert(i, garbage.toString());
                i += garbageLen;
            }
        }
        for (int i = 0; i < sb.length(); i++) {
            if (random.nextDouble() < obfuscateChance) {
                int len = 1 + random.nextInt(4);
                String obfuscated = "§k" + sb.substring(i, Math.min(i + len, sb.length())) + "§r";
                sb.replace(i, Math.min(i + len, sb.length()), obfuscated);
                i += obfuscated.length();
            }
        }
        return sb.toString();
    }

    public static String getMessage(ItemStack item) {
        if (!isDiskette(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(messageKey, PersistentDataType.STRING);
    }

    public static void setQuality(ItemStack item, String quality) {
        // Используем нейтральный префикс без локали для поиска строки в lore
        setLoreLine(item, "Качество:", Lang.get("diskette.quality_prefix") + quality, null, Integer.MAX_VALUE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, quality);
            item.setItemMeta(meta);
        }
    }

    public static boolean upgradeQuality(ItemStack item) {
        String current = getQuality(item);
        if (current == null) return false;
        int index = QUALITIES.indexOf(current);
        if (index == -1 || index == QUALITIES.size() - 1) return false;
        setQuality(item, QUALITIES.get(index + 1));
        return true;
    }

    public static boolean hasHash(ItemStack item) {
        if (!isDiskette(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;
        for (String line : lore) {
            if (line == null) continue;
            String plain = COLOR_PATTERN.matcher(line).replaceAll("");
            if (plain.startsWith("Хэш:[")) {
                return !plain.equals("Хэш:[NULL]");
            }
        }
        return false;
    }

    private static void registerRecipe(JavaPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "diskette_recipe");
        plugin.getServer().removeRecipe(key);

        ItemStack result = createDiskette();
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(1, Material.COPPER_INGOT);
        recipe.addIngredient(1, Material.GOLD_INGOT);
        recipe.addIngredient(2, Material.REDSTONE);
        plugin.getServer().addRecipe(recipe);
    }
}
