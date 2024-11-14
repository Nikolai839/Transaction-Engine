package dk.superawesome.command;

import dk.superawesome.gui.ESettingsGui;
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
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cKun spillere kan gøre dette!");
            return false;
        }

        sender.sendMessage("§eÅbner transaktions-menuen op...");
        new ESettingsGui().open(player);
        return false;
    }
}
