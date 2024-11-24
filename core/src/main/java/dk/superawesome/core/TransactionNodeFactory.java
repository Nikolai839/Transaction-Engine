package dk.superawesome.core;

import dk.superawesome.core.db.Settings;
import dk.superawesome.core.exceptions.RequestException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.Date;

public class TransactionNodeFactory implements NodeFactory<SingleTransactionNode> {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Copenhagen");

    public static final String TIME = "time";
    public static final String AMOUNT = "amount";
    public static final String FROM_USER = "from_user";
    public static final String TO_USER = "to_user";
    public static final String FROM_USER_PRE_BALANCE = "from_user_pre_balance";
    public static final String TO_USER_PRE_BALANCE = "to_user_pre_balance";
    public static final String PAY_TYPE = "pay_type";
    public static final String EXTRA = "extra";

    private final String timeKey;
    private final String amountKey;
    private final String fromUserKey;
    private final String toUserKey;
    private final String fromUserPreBalanceKey;
    private final String toUserPreBalanceKey;
    private final String typeKey;
    private final String extraKey;

    public TransactionNodeFactory(Settings settings) {
        this.timeKey = settings.get(TIME);
        this.amountKey = settings.get(AMOUNT);
        this.fromUserKey = settings.get(FROM_USER);
        this.toUserKey = settings.get(TO_USER);
        this.fromUserPreBalanceKey = settings.get(FROM_USER_PRE_BALANCE);
        this.toUserPreBalanceKey = settings.get(TO_USER_PRE_BALANCE);
        this.typeKey = settings.get(PAY_TYPE);
        this.extraKey = settings.get(EXTRA);
    }

    @Override
    public SingleTransactionNode createNode(ResultSet set) throws RequestException {
        try {
            return new SingleTransactionNode(
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(set.getTimestamp(timeKey).getTime()), ZoneOffset.UTC)
                            .withZoneSameInstant(ZONE_ID),
                    set.getDouble(amountKey),
                    set.getString(fromUserKey),
                    set.getString(toUserKey),
                    set.getDouble(fromUserPreBalanceKey),
                    set.getDouble(toUserPreBalanceKey),
                    TransactionNode.PayType.valueOf(set.getString(typeKey).toUpperCase()),
                    set.getString(extraKey)
            );
        } catch (SQLException ex) {
            throw new RequestException(ex);
        }
    }
}
