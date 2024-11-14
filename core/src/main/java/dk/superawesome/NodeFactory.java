package dk.superawesome;

import dk.superawesome.exceptions.RequestException;

import java.sql.ResultSet;

public interface NodeFactory<N extends Node> {

    N createNode(ResultSet set) throws RequestException;
}
