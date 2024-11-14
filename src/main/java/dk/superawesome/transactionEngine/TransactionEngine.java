package dk.superawesome.transactionEngine;

import org.bukkit.plugin.java.JavaPlugin;

public final class TransactionEngine extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic


        EngineQuery<TransactionNode> query = Engine.query(EngineRequest.Builder.makeRequest()
                .build())
                .transform(PostQueryTransformer.GroupBy.<TransactionNode>groupBy(TransactionNode::toUserName, Object::equals));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
