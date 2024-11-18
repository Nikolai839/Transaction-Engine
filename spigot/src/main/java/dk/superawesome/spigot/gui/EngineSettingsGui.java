package dk.superawesome.spigot.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import dk.superawesome.core.exceptions.RequestException;
import dk.superawesome.spigot.DatabaseController;
import dk.superawesome.spigot.TransactionEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class EngineSettingsGui {

    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);
    private final Gui gui;

    private SortingMethod sortingMethod = SortingMethod.BY_TIME;
    private boolean traceModeEnabled;
    private boolean groupUserNamesEnabled;
    private int groupUserNamesMax = -1;
    private boolean groupUserNamesFrom;
    private final List<String> toUserNames = new ArrayList<>();
    private final List<String> fromUserNames = new ArrayList<>();
    private double amountFrom;
    private double amountTo;
    private Date timeFrom;
    private Date timeTo;

    public EngineSettingsGui() {
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (Indstillinger)"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i : Arrays.asList(7, 8, 16, 17, 25, 26, 34, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52)) {
            gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }
        for (int i : Arrays.asList(9, 10, 11, 12, 13, 14, 15, 27, 28, 29, 30, 31, 32, 33, 37, 39)) {
            gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)));
        }

        gui.setItem(0, new GuiItem(Material.SIGN, event -> addToUser((Player) event.getWhoClicked())));
        gui.setItem(18, new GuiItem(Material.SIGN, event -> addFromUser((Player) event.getWhoClicked())));
        gui.setItem(36, new GuiItem(Material.GOLD_INGOT, event -> configureAmountRange((Player) event.getWhoClicked())));
        gui.setItem(38, new GuiItem(Material.COMPASS, event -> configureTimeRange((Player) event.getWhoClicked())));
        gui.setItem(40, new GuiItem(Material.LEATHER_BOOTS, __ -> configureGrouped()));
        gui.setItem(41, new GuiItem(Material.ENDER_PEARL, __ -> configureTraceMode()));
        gui.setItem(42, new GuiItem(Material.BOOK_AND_QUILL, __ -> changeSortingMethod()));
        gui.setItem(35, new GuiItem(new ItemStack(Material.WOOL, 1, (short) 14), __ -> resetSettings()));
        gui.setItem(53, new GuiItem(new ItemStack(Material.WOOL, 1, (short) 5), event -> openEngineGui((Player) event.getWhoClicked())));
    }

    public void open(Player player) {
        gui.open(player);
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
        this.sortingMethod = SortingMethod.BY_TIME;
        this.traceModeEnabled = false;
        this.groupUserNamesEnabled = false;
        this.groupUserNamesMax = -1;
        this.groupUserNamesFrom = false;
        this.toUserNames.clear();
        this.fromUserNames.clear();
        this.amountFrom = 0;
        this.amountTo = 0;
        this.timeFrom = null;
        this.timeTo = null;
    }

    private void openEngineGui(Player player) {
        try {

            EngineLoadingGui gui = new EngineLoadingGui();
            gui.open(player);
            Consumer<BukkitRunnable> callback = task -> {
                // make sure the player has not closed the inventory while loading
                if (!gui.isTaskCancelled()) {
                    player.closeInventory();
                    task.runTask(TransactionEngine.instance);
                }
            };

            THREAD_POOL.submit(() -> openEngineGuiAsync(player, callback));

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred when submitting task to thread pool", ex);
            player.sendMessage("§cDer skete en fejl! Kontakt en udvikler!");
        }
    }

    private void openEngineGuiAsync(Player player, Consumer<BukkitRunnable> callback) {
        try {
            DatabaseController controller = TransactionEngine.instance.getDatabaseController();
            TransactionRequestBuilder builder = EngineRequest.Builder.makeRequest(TransactionRequestBuilder.class, TransactionEngine.instance.getSettings(), controller, controller.getRequester());

            builder.to(toUserNames.toArray(String[]::new));
            builder.from(fromUserNames.toArray(String[]::new));
            if (this.amountFrom != 0) {
                builder.from(this.amountFrom);
            }
            if (this.amountTo != 0) {
                builder.from(this.amountTo);
            }
            if (this.timeFrom != null) {
                builder.from(this.timeFrom);
            }
            if (this.timeTo != null) {
                builder.to(this.timeTo);
            }

            EngineQuery<SingleTransactionNode> query = Engine.query(builder.build());
            Node.Collection collection = Node.Collection.SINGLE;
            if (this.traceModeEnabled) {
                collection = Node.Collection.GROUPED;

                // TODO
            }

            EngineQuery<? extends TransactionNode> finalQuery;
            if (this.groupUserNamesEnabled) {
                collection = Node.Collection.GROUPED;

                Function<SingleTransactionNode, Object> func;
                TransactionNode.GroupedTransactionNode.Bound bound;
                if (this.groupUserNamesFrom) {
                    bound = TransactionNode.GroupedTransactionNode.Bound.FROM;
                    func = SingleTransactionNode::fromUserName;
                } else {
                    bound = TransactionNode.GroupedTransactionNode.Bound.TO;
                    func = SingleTransactionNode::toUserName;
                }

                if (this.groupUserNamesMax == -1) {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.<SingleTransactionNode, TransactionNode.GroupedTransactionNode>groupBy(func, Object::equals, nodes -> new TransactionNode.GroupedTransactionNode(nodes, bound)));
                } else {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.<SingleTransactionNode, TransactionNode.GroupedTransactionNode>groupBy(func, Object::equals, PostQueryTransformer.GroupBy.GroupOperator.max(this.groupUserNamesMax), nodes -> new TransactionNode.GroupedTransactionNode(nodes, bound)));
                }
            } else {
                finalQuery = query;
            }

            finalQuery = Engine.doTransformation(collection, this.sortingMethod, finalQuery);
            EngineQuery<? extends TransactionNode> queryBuffer = finalQuery;
            callback.accept(new BukkitRunnable() {

                @Override
                public void run() {
                    new EngineGui<>(queryBuffer)
                            .open(player);
                }
            });

        } catch (RequestException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Faild to query", ex);
        }
    }
}
