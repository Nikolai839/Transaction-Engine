package dk.superawesome.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.*;
import dk.superawesome.exceptions.RequestException;
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

    private static final List<PostQueryTransformer<TransactionNode, List<TransactionNode>>> sortingMethods = Arrays.asList(SortingMethods.BY_TIME);
    private final Gui gui;

    private int sortingMethod = sortingMethods.indexOf(SortingMethods.BY_TIME);
    private boolean traceModeEnabled;
    private boolean groupUserNamesEnabled;
    private int groupUserNamesMax = -1;
    private boolean groupUserNamesFrom;
    private List<String> toUserNames = new ArrayList<>();
    private List<String> fromUserNames = new ArrayList<>();
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
        this.sortingMethod = sortingMethods.indexOf(SortingMethods.BY_TIME);
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

    @SuppressWarnings("unchecked")
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

            EngineQuery<SimpleTransactionNode> query = Engine.query(builder.build());
            if (this.traceModeEnabled) {
                // TODO
            }

            EngineQuery<? extends TransactionNode> finalQuery;
            if (this.groupUserNamesEnabled) {
                Function<SimpleTransactionNode, Object> func;
                if (this.groupUserNamesFrom) {
                    func = SimpleTransactionNode::fromUserName;
                } else {
                    func = SimpleTransactionNode::toUserName;
                }

                if (this.groupUserNamesMax == -1) {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.groupBy(func, Object::equals, SimpleTransactionNode.COLLECTOR));
                } else {
                    finalQuery = query.transform(PostQueryTransformer.GroupBy.groupBy(func, Object::equals, PostQueryTransformer.GroupBy.GroupOperator.max(this.groupUserNamesMax), SimpleTransactionNode.COLLECTOR));
                }
            } else {
                finalQuery = query;
            }

            new EngineGui(((EngineQuery<TransactionNode>)finalQuery).transform(sortingMethods.get(this.sortingMethod)))
                    .open(player);

        } catch (RequestException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Faild to query", ex);
        }
    }
}
