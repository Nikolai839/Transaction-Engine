package dk.superawesome;

import dk.superawesome.db.Settings;
import dk.superawesome.exceptions.RequestException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionNodeFactory implements NodeFactory<SimpleTransactionNode> {

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
    public SimpleTransactionNode createNode(ResultSet set) throws RequestException {
        try {
            return new SimpleTransactionNode(new Date(set.getDate(timeKey).getTime()), set.getDouble(amountKey), set.getString(fromUserKey), set.getString(toUserKey));
        } catch (SQLException ex) {
            throw new RequestException(ex);
        }
    }
}
