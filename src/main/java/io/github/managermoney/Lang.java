package io.github.managermoney;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Менеджер локализации / Localization manager.
 * Читает файл lang/<locale>.yml, подставляет значения по ключу.
 * Поддерживаемые локали: ru, en.
 */
public class Lang {

    private static FileConfiguration messages;

    /** Инициализировать, вызывать в Main.onEnable() после saveDefaultConfigs(). */
    public static void init(JavaPlugin plugin) {
        String locale = plugin.getConfig().getString("locale", "ru");
        File langFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
        }

        // Загружаем файл из папки плагина
        messages = YamlConfiguration.loadConfiguration(langFile);

        // Используем встроенный файл как запасной вариант (defaults)
        InputStream defaultStream = plugin.getResource("lang/" + locale + ".yml");
        if (defaultStream == null) {
            // Если запрошенная локаль не встроена — фолбэк на ru
            defaultStream = plugin.getResource("lang/ru.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }
    }

    /**
     * Получить строку по ключу.
     * Поддерживает §-цвета прямо в YAML.
     * @param key  путь в YAML, например "telescope.built"
     * @param args пары "placeholder", "value" для замены {placeholder}
     */
    public static String get(String key, Object... args) {
        String value = messages.getString(key);
        if (value == null) return "§c[Missing: " + key + "]";
        value = value.replace("&", "§");          // поддержка & в YAML
        if (args.length % 2 != 0) return value;
        for (int i = 0; i < args.length; i += 2) {
            value = value.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return value;
    }

    /** Получить список строк по ключу (для lore). */
    public static List<String> getList(String key, Object... args) {
        List<String> list = messages.getStringList(key);
        for (int j = 0; j < list.size(); j++) {
            String line = list.get(j).replace("&", "§");
            if (args.length % 2 == 0) {
                for (int i = 0; i < args.length; i += 2) {
                    line = line.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
                }
            }
            list.set(j, line);
        }
        return list;
    }
}
