package dk.superawesome.spigot.command;

import dk.superawesome.spigot.gui.EngineSettingsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalEngineCommand implements CommandExecutor {

    private static final String PERMISSION = "balengine.usage";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cDu har ikke adgang til dette!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cKun spillere kan gøre dette!");
            return true;
        }

        player.closeInventory();
        new EngineSettingsGui().open(player);
        return true;
    }
}
