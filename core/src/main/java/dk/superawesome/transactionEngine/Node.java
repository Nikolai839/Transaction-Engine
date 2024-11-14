package dk.superawesome.transactionEngine;

import java.util.Date;

public interface Node {

    interface Timed extends Node {

        Date time();
    }
}
