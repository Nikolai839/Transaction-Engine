package dk.superawesome.spigot;

import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.spigot.command.BalEngineCommand;
import dk.superawesome.spigot.gui.EngineSettingsGui;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class TransactionEngine extends JavaPlugin {

    private DatabaseSettings settings;
    private final DatabaseController databaseController = new DatabaseController();

    public static TransactionEngine instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        this.settings = new DatabaseSettings(
                config.getString("db.host"),
                config.getInt("db.port"),
                config.getString("db.username"),
                config.getString("db.password"),
                config.getString("db.database")
        );

        PluginCommand command = getCommand("transaktioner");
        if (command != null) {
            command.setExecutor(new BalEngineCommand());
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, EngineSettingsGui::loadToCache);
    }

    public DatabaseSettings getSettings() {
        return this.settings;
    }

    public DatabaseController getDatabaseController() {
        return this.databaseController;
    }
}
