package dk.superawesome.transactionEngine;

import java.util.Date;

public record TransactionNode(Date time, double amount, String fromUserName, String toUserName) implements Node {
}
