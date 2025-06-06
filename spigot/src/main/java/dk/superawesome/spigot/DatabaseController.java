package dk.superawesome.spigot;

import dk.superawesome.core.*;
import dk.superawesome.core.db.DatabaseExecutor;
import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.core.db.Requester;
import dk.superawesome.core.db.Settings;
import dk.superawesome.core.exceptions.RequestException;
import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.TransactionNodeFactory;
import org.bukkit.Bukkit;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.logging.Level;

public class DatabaseController implements DatabaseExecutor<SingleTransactionNode> {

    private static final int MAX_POOL_SIZE = 10;

    private final MariaDbPoolDataSource source = new MariaDbPoolDataSource();
    private boolean hasAppliedSettings;

    private final Requester requester;
    private final TransactionNodeFactory nodeFactory;
    {
        this.nodeFactory = new TransactionNodeFactory(new Settings.Mapped(new HashMap<>(){{
            put(TransactionNodeFactory.TIME, "created");
            put(TransactionNodeFactory.AMOUNT, "amount");
            put(TransactionNodeFactory.FROM_USER, "fromplayer");
            put(TransactionNodeFactory.TO_USER, "toplayer");
            put(TransactionNodeFactory.FROM_USER_PRE_BALANCE, "fromplayer_pre_balance");
            put(TransactionNodeFactory.TO_USER_PRE_BALANCE, "toplayer_pre_balance");
            put(TransactionNodeFactory.PAY_TYPE, "paytype");
            put(TransactionNodeFactory.EXTRA, "extra");
        }}));

        this.requester = new Requester() {
            @Override
            public String getQuery() {
                return """
                        SELECT l.created, l.amount AS amount, p1.username as fromplayer, p2.username as toplayer, l.fromplayer_pre_balance, l.toplayer_pre_balance, l.paytype, l.extra
                        FROM ems_log l
                        INNER JOIN players p1 ON p1.id = l.fromplayer
                        INNER JOIN players p2 ON p2.id = l.toplayer
                        WHERE p1.username != p2.username
                        ORDER BY l.created DESC;
                       """;
            }

            @Override
            public String getQuery(LocalDateTime dateTime) {
                String time = dateTime.getYear() + "-" + dateTime.getMonthValue() + "-" + dateTime.getDayOfMonth() + " " + dateTime.getHour() + ":" + dateTime.getMinute() + ":" + dateTime.getSecond();

                return String.format("""
                        SELECT l.created, l.amount AS amount, p1.username as fromplayer, p2.username as toplayer, l.fromplayer_pre_balance, l.toplayer_pre_balance, l.paytype, l.extra
                        FROM ems_log l
                        INNER JOIN players p1 ON p1.id = l.fromplayer
                        INNER JOIN players p2 ON p2.id = l.toplayer
                        WHERE p1.username != p2.username AND l.created > CAST('%s' AS DATETIME)
                        ORDER BY l.created DESC;
                        """, time);
            }
        };
    }

    public Requester getRequester() {
        return this.requester;
    }

    private void applySettings(DatabaseSettings settings) throws SQLException {
        this.source.setUser(settings.username());
        this.source.setPassword(settings.password());
        this.source.setUrl("jdbc:mariadb://" +  settings.host() + ":" + settings.port() + "/" + settings.database() + "?maxPoolSize=" + MAX_POOL_SIZE);
        this.hasAppliedSettings = true;
    }

    @Override
    public EngineQuery<SingleTransactionNode> execute(EngineCache<SingleTransactionNode> cache, DatabaseSettings settings, String query) throws RequestException, SQLException {
        if (!this.hasAppliedSettings) {
            try {
                applySettings(settings);
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to apply settings to database pool source.", ex);
                return null;
            }
        }

        try (ResultSet set = runQuery(query)) {
            return EngineQuery.create(set, this.nodeFactory, cache);
        }
    }

    public ResultSet runQuery(String sql, Object... values) throws SQLException {
        ResultSet rs = null;
        try (Connection conn = this.source.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

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
