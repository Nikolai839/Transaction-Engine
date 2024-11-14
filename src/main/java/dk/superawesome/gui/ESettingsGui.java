package dk.superawesome.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ESettingsGui extends AbstractGui {

    private final Gui gui;

    public ESettingsGui() {
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (Indstillinger)"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i : Arrays.asList(7, 8, 16, 17, 25, 26, 34, 43, 44, 45, 46, 47, 48, 49, 50, 51)) {
            gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }
        for (int i : Arrays.asList(9, 10, 11, 12, 13, 14, 15, 27, 28, 29, 30, 31, 32, 33, 37, 39)) {
            gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)));
        }

        gui.setItem(0, new GuiItem(Material.SIGN));
        gui.addSlotAction(0, event -> addToUser((Player) event.getWhoClicked()));

        gui.setItem(18, new GuiItem(Material.SIGN));
        gui.addSlotAction(18, event -> addFromUser((Player) event.getWhoClicked()));

        gui.setItem(36, new GuiItem(Material.GOLD_INGOT));
        gui.addSlotAction(36, event -> configureAmountRange((Player) event.getWhoClicked()));

        gui.setItem(38, new GuiItem(Material.COMPASS));
        gui.addSlotAction(38, event -> configureTimeRange((Player) event.getWhoClicked()));

        gui.setItem(40, new GuiItem(Material.LEATHER_BOOTS));
        gui.addSlotAction(40, __ -> configureGrouped());

        gui.setItem(41, new GuiItem(Material.ENDER_PEARL));
        gui.addSlotAction(41, __ -> configureTraceMode());

        gui.setItem(42, new GuiItem(Material.BOOK_AND_QUILL));
        gui.addSlotAction(42, __ -> changeSortingMethod());

        gui.setItem(35, new GuiItem(new ItemStack(Material.WOOL, 1, (short) 14)));
        gui.addSlotAction(35, __ -> resetSettings());

        gui.setItem(53, new GuiItem(new ItemStack(Material.WOOL, 1, (short) 5)));
        gui.addSlotAction(53, event -> openEngineGui((Player) event.getWhoClicked()));
    }

    private void changeSortingMethod() {

    }

    private void configureTraceMode() {

    }

    private void configureGrouped() {

    }

    private void addToUser(Player player) {

    }

    private void addFromUser(Player player) {

    }

    private void configureAmountRange(Player player) {

    }

    private void configureTimeRange(Player player) {

    }

    private void resetSettings() {

    }

    public void open(Player player) {

    }

    private void openEngineGui(Player player) {

    }
}
