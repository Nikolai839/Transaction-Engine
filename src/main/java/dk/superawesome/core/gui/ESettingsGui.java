package dk.superawesome.core.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import dk.superawesome.core.exceptions.RequestException;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

public class ESettingsGui extends AbstractGui {

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
            TransactionRequestBuilder builder = EngineRequest.Builder.makeRequest(TransactionRequestBuilder.class, TransactionEngine.instance.getSettings(), TransactionEngine.instance.getDatabaseController(), () -> null);
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
                // TODO
                collection = Node.Collection.GROUPED;
            }

            EngineQuery<? extends TransactionNode> finalQuery;
            if (this.groupUserNamesEnabled) {
                Function<SingleTransactionNode, Object> func;
                if (this.groupUserNamesFrom) {
                    func = SingleTransactionNode::fromUserName;
                } else {
                    func = SingleTransactionNode::toUserName;
                }

                if (this.groupUserNamesMax == -1) {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.groupBy(func, Object::equals, TransactionNode.GroupedTransactionNode::new));
                } else {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.groupBy(func, Object::equals, PostQueryTransformer.GroupBy.GroupOperator.max(this.groupUserNamesMax), TransactionNode.GroupedTransactionNode::new));
                }
            } else {
                finalQuery = query;
            }

            new EngineGui(doSort(collection, finalQuery))
                    .open(player);

        } catch (RequestException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Faild to query", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <N extends TransactionNode, V extends PostQueryTransformer.SortBy.SortVisitor<N>> EngineQuery<N> doSort(Node.Collection collection, EngineQuery<N> query) {
        PostQueryTransformer.SortBy.SortVisitor<N> visitor = PostQueryTransformer.SortBy.getVisitor(collection);
        PostQueryTransformer<N, N> sort = collection.<N, V>getComparator().visit(this.sortingMethod, (V) visitor);

        return query.transform(sort);
    }
}
