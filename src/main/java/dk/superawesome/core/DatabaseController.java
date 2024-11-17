package dk.superawesome.core;

import dk.superawesome.core.db.DatabaseExecutor;
import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.core.db.Requester;
import dk.superawesome.core.db.Settings;
import dk.superawesome.core.exceptions.RequestException;
import org.bukkit.Bukkit;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;

public class DatabaseController implements DatabaseExecutor<SingleTransactionNode> {

    private static final int MAX_POOL_SIZE = 10;

    private final MariaDbPoolDataSource source = new MariaDbPoolDataSource();
    private boolean hasAppliedSettings;

    private final TransactionNodeFactory nodeFactory;
    {
        this.nodeFactory = new TransactionNodeFactory(new Settings.Mapped(new HashMap<>(){{
            put(TransactionNodeFactory.TIME, "created");
            put(TransactionNodeFactory.AMOUNT, "amount");
            put(TransactionNodeFactory.FROM_USER, "fromplayer");
            put(TransactionNodeFactory.TO_USER, "toplayer");
        }}));
    }

    private void applySettings(DatabaseSettings settings) throws SQLException {
        this.source.setUser(settings.username());
        this.source.setPassword(settings.password());
        this.source.setUser("jdbc:mariadb://" +  settings.host() + ":" + settings.port() + "/" + settings.database() + "?maxPoolSize=" + MAX_POOL_SIZE);
        this.hasAppliedSettings = true;
    }

    @Override
    public EngineQuery<SingleTransactionNode> execute(DatabaseSettings settings, Requester requester) throws RequestException, SQLException {
        if (!this.hasAppliedSettings) {
            try {
                applySettings(settings);
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to apply settings to database pool source.", ex);
                return null;
            }
        }

        try (ResultSet set = runQuery(requester.toQuery())) {
            return EngineQuery.create(set, this.nodeFactory);
        }
    }

    public ResultSet runQuery(String sql, Object... values) throws SQLException {
        ResultSet rs = null;
        try (Connection conn = this.source.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {

            for (int i = 0; i < values.length; i++) {
                stmt.setObject(i + 1, values[i]);
            }
            rs = stmt.executeQuery();
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return rs;
    }
}
