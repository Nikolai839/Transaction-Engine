package dk.superawesome;

import java.util.Date;

public record TransactionNode(Date time, double amount, String fromUserName, String toUserName) implements Node.Timed {
}
