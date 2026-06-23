package io.github.managermoney;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ObservatoryListener observatoryListener;
    private DisketteConsoleListener consoleListener;

    @Override
    public void onEnable() {
        // 1. Сохранить дефолтные конфиги
        saveDefaultConfig();
        saveResource("lang/ru.yml", false);
        saveResource("lang/en.yml", false);
        saveResource("signals.yml", false);

        // 2. Инициализировать локализацию
        Lang.init(this);

        // 3. Инициализировать менеджеры
        DisketteManager.init(this);
        SignalSourceManager.init(this);

        getLogger().info(Lang.get("plugin.enabled"));

        TelescopeListener telescopeListener = new TelescopeListener(this);
        TelescopeGUI telescopeGUI = new TelescopeGUI(this, telescopeListener);
        telescopeListener.setGui(telescopeGUI);
        getServer().getPluginManager().registerEvents(telescopeListener, this);
        getServer().getPluginManager().registerEvents(telescopeGUI, this);

        ObservatoryGUI observatoryGUI = new ObservatoryGUI(this, telescopeGUI, telescopeListener);
        observatoryListener = new ObservatoryListener(observatoryGUI, this);
        observatoryGUI.setObservatoryListener(observatoryListener);
        getServer().getPluginManager().registerEvents(observatoryListener, this);
        getServer().getPluginManager().registerEvents(observatoryGUI, this);

        DisketteConsoleGUI consoleGUI = new DisketteConsoleGUI(this);
        consoleListener = new DisketteConsoleListener(consoleGUI, this);
        consoleGUI.setConsoleListener(consoleListener);
        getServer().getPluginManager().registerEvents(consoleListener, this);
        getServer().getPluginManager().registerEvents(consoleGUI, this);

        getServer().getPluginManager().registerEvents(new DisketteListener(), this);
        getServer().getPluginManager().registerEvents(new CashierListener(this), this);

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning(Lang.get("plugin.no_vault"));
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (observatoryListener != null) observatoryListener.saveObservatories();
        if (consoleListener != null)     consoleListener.saveConsoles();
        getLogger().info(Lang.get("plugin.disabled"));
    }
}
