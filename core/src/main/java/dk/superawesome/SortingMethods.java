package dk.superawesome;

import java.util.Date;

public class SortingMethods {

    public static PostQueryTransformer<SimpleTransactionNode> BY_TIME = PostQueryTransformer.SortBy.sortBy(SimpleTransactionNode::time, Date::compareTo);
}
