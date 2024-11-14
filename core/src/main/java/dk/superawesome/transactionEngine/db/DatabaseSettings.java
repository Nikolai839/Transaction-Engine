package dk.superawesome.transactionEngine.db;

public record DatabaseSettings(String host, int port, String username, String password, String database) {

}
