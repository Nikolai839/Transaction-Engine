package dk.superawesome.spigot.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.spigot.TransactionEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class EngineLoadingGui implements Listener {

    private final Gui gui;
    private final BukkitTask task;

    private boolean taskCancelled;
    private int nextStartSlot;

    public EngineLoadingGui() {
        this.gui = Gui.gui()
                .title(Component.text("Indlæser...."))
                .rows(1)
                .disableAllInteractions()
                .create();

        for (int i = 3; i < 9; i++) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)));
        }

        for (int i = 0; i < 3; i++) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (nextStartSlot > 0) {
                    gui.updateItem((nextStartSlot - 1) % 9, new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7));
                }
                for (int i = nextStartSlot++; i < nextStartSlot + 2; i++) {
                    gui.updateItem(i % 9, new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15));
                }
            }
        };

        this.task = runnable.runTaskTimer(TransactionEngine.instance, 0L, 5L);

        Bukkit.getPluginManager().registerEvents(this, TransactionEngine.instance);
    }

    public boolean isTaskCancelled() {
        return this.taskCancelled;
    }

    public void open(Player player) {
        this.gui.open(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == this.gui.getInventory()) {
            this.task.cancel();
            this.taskCancelled = true;
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getInventory() == this.gui.getInventory()) {
            this.task.cancel();
            this.taskCancelled = true;
            HandlerList.unregisterAll(this);
        }
    }
}
