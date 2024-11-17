package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;

import java.sql.ResultSet;

public interface NodeFactory<N extends Node> {

    N createNode(ResultSet set) throws RequestException;
}
