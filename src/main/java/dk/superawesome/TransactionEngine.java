package dk.superawesome;

import dk.superawesome.db.DatabaseExecutor;
import dk.superawesome.db.DatabaseSettings;
import dk.superawesome.exceptions.RequestException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TransactionEngine extends JavaPlugin {

    private DatabaseSettings settings;
    private final DatabaseController databaseController = new DatabaseController();

    @Override
    public void onEnable() {
        // Plugin startup logic

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        this.settings = new DatabaseSettings(
                config.getString("db.host"),
                config.getInt("db.port"),
                config.getString("db.username"),
                config.getString("db.password"),
                config.getString("db.database")
        );

        try {
            EngineQuery<TransactionNode> query = Engine.query(EngineRequest.Builder.makeRequest(TransactionRequestBuilder.class, this.settings, this.databaseController, () -> null)
                    .build())
                    .transform(PostQueryTransformer.GroupBy.groupBy(TransactionNode::toUserName, Object::equals));

        } catch (RequestException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Faild to query", ex);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public DatabaseSettings getSettings() {
        return this.settings;
    }

    public DatabaseController getDatabaseController() {
        return this.databaseController;
    }
}
