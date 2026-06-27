// Вставить рядом с остальными классами в пакет io.github.managermoney
package io.github.managermoney;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SignalSourceManager {

    public static class SignalSource {
        private final String id;
        private final String displayName;
        private final List<String> messages;
        private final int valueBonus;

        public SignalSource(String id, String displayName, List<String> messages, int valueBonus) {
            this.id          = id;
            this.displayName = displayName;
            this.messages    = Collections.unmodifiableList(messages);
            this.valueBonus  = valueBonus;
        }

        public String getId()          { return id; }
        public String getDisplayName() { return displayName; }
        public int    getValueBonus()  { return valueBonus; }

        public String randomMessage() {
            if (messages.isEmpty()) return "";
            return messages.get(new Random().nextInt(messages.size()));
        }
    }

    private static final List<SignalSource>        allSources   = new ArrayList<>();
    private static final Map<String, SignalSource> byName       = new HashMap<>();
    private static final Random                    RANDOM       = new Random();
    private static SignalSource                    transmitter  = null;

    public static void init(JavaPlugin plugin) {
        allSources.clear();
        byName.clear();
        transmitter = null;

        File file = new File(plugin.getDataFolder(), "signals.yml");
        if (!file.exists()) {
            plugin.saveResource("signals.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Defaults из jar
        InputStream defStream = plugin.getResource("signals.yml");
        if (defStream != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }

        // signals.yml использует формат "KEY: {name:..., messages:[...]}"
        // а не список "- id: KEY", поэтому читаем через getConfigurationSection
        org.bukkit.configuration.ConfigurationSection sourcesSection = cfg.getConfigurationSection("sources");
        if (sourcesSection != null) {
            for (String id : sourcesSection.getKeys(false)) {
                try {
                    org.bukkit.configuration.ConfigurationSection entry = sourcesSection.getConfigurationSection(id);
                    if (entry == null) continue;

                    String displayName = entry.getString("name", id);
                    int    valueBonus  = entry.getInt("value_bonus", 0);

                    List<String> messages = new ArrayList<>();
                    List<?> rawMsgs = entry.getList("messages");
                    if (rawMsgs != null) {
                        for (Object o : rawMsgs) messages.add(String.valueOf(o));
                    }

                    SignalSource src = new SignalSource(id, displayName, messages, valueBonus);
                    allSources.add(src);
                    byName.put(displayName, src);

                    if ("transmitter".equalsIgnoreCase(id)) {
                        transmitter = src;
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("[SignalSourceManager] Ошибка загрузки источника '" + id + "': " + ex.getMessage());
                }
            }
        }

        plugin.getLogger().info("[SignalSourceManager] Загружено источников: " + allSources.size());
    }

    /** Найти источник по отображаемому имени (используется в DisketteManager). */
    public static SignalSource getByDisplayName(String displayName) {
        return byName.get(displayName);
    }

    /** Случайный источник (TRANSMITTER выпадает редко, 1/250). */
    public static SignalSource random() {
        if (transmitter != null && RANDOM.nextInt(250) == 0) {
            return transmitter;
        }
        List<SignalSource> pool = new ArrayList<>(allSources);
        if (transmitter != null) pool.remove(transmitter);
        if (pool.isEmpty()) return transmitter; // fallback
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    public static List<SignalSource> getAll() {
        return Collections.unmodifiableList(allSources);
    }
}