package dk.superawesome.core;

import dk.superawesome.core.db.Settings;
import dk.superawesome.core.exceptions.RequestException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class TransactionNodeFactory implements NodeFactory<SingleTransactionNode> {

    public static final String TIME = "time";
    public static final String AMOUNT = "amount";
    public static final String FROM_USER = "from_user";
    public static final String TO_USER = "to_user";

    private final String timeKey;
    private final String amountKey;
    private final String fromUserKey;
    private final String toUserKey;

    public TransactionNodeFactory(Settings settings) {
        this.timeKey = settings.get(TIME);
        this.amountKey = settings.get(AMOUNT);
        this.fromUserKey = settings.get(FROM_USER);
        this.toUserKey = settings.get(TO_USER);
    }

    @Override
    public SingleTransactionNode createNode(ResultSet set) throws RequestException {
        try {
            return new SingleTransactionNode(
                    ZonedDateTime.ofInstant(new Date(set.getDate(timeKey).getTime()).toInstant(), ZoneId.of("Europe/Copenhagen")),
                    set.getDouble(amountKey),
                    set.getString(fromUserKey),
                    set.getString(toUserKey));
        } catch (SQLException ex) {
            throw new RequestException(ex);
        }
    }
}
