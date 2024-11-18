package dk.superawesome.spigot.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EngineGui<N extends TransactionNode> {

    private static final DecimalFormat AMOUNT_FORMATTER = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.ENGLISH));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Gui gui;
    private final QueryContext<N> context;
    private final EngineSettingsGui settings;
    private TransactionVisitor<N> visitor;

    private boolean hasDisplayedInitial;
    private int scrolledDown;

    private record QueryContext<CN extends TransactionNode>(QueryContext<CN> previousContext, EngineQuery<CN> query) {

    }

    public EngineGui(EngineQuery<N> query, EngineSettingsGui settings) {
        this(query, null, settings);
    }

    private EngineGui(EngineQuery<N> query, QueryContext<N> previousContext, EngineSettingsGui settings) {
        this.context = new QueryContext<>(previousContext, query);
        this.settings = settings;
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (" + query.size() + ")"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i : Arrays.asList(8, 17, 26, 35, 47, 48, 49, 50, 51, 52)) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }

        if (previousContext != null) {
            this.gui.setItem(45, new GuiItem(
                    ItemBuilder.from(Material.ARROW)
                            .name(Component.text("§6Gå tilbage"))
                            .build(), event -> clickBack((Player) event.getWhoClicked())));
        }
        this.gui.setItem(44, new GuiItem(
                ItemBuilder.from(Material.ARROW)
                        .name(Component.text("§6Rul opad"))
                        .build(), __ -> clickUp()));

        this.gui.setItem(53, new GuiItem(
                ItemBuilder.from(Material.ARROW)
                        .name(Component.text("§6Rul nedad"))
                        .build(), __ -> clickDown()));

        this.gui.setItem(46, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 14))
                        .name(Component.text("§cTilbage til indstillinger"))
                        .build(), event -> clickNewSettings((Player) event.getWhoClicked())));

        displayNodes();
    }

    public void open(Player player) {
        this.gui.open(player);
    }

    @SuppressWarnings("unchecked")
    private TransactionVisitor<N> getVisitor(TransactionNode node) {
        if (this.visitor != null) {
            return this.visitor;
        }

        if (node.isGrouped()) {
            return this.visitor = (TransactionVisitor<N>) new GroupedTransactionVisitor();
        } else {
            return this.visitor = (TransactionVisitor<N>) new SingleTransactionVisitor();
        }
    }

    private void displayNodes() {
        if (hasDisplayedInitial && !this.context.query().isEmpty()) {
            // clear previous items
            for (int i = 1; i < 8; i++) {
                for (int j = 1; j < 6; j++) {
                    this.gui.setItem(j, i, new GuiItem(Material.AIR));
                }
            }
        }

        int i = 0, c = 0, f = 0;
        for (N node : this.context.query().nodes()) {
            if (c >= scrolledDown) {
                TransactionVisitor<N> visitor = getVisitor(node);

                ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 0, (byte) SkullType.PLAYER.ordinal());
                visitor.applyToItem(node, item, f);

                int slot = (c - scrolledDown) * 9 + i;
                this.gui.setItem(slot, new GuiItem(item, event -> clickInspection((Player) event.getWhoClicked(), node)));
            }

            f++;
            i++;
            if (i > 7) {
                c++;
                i = 0;
            }

            if (c - scrolledDown > 4) {
                break;
            }
        }

        gui.update();
        hasDisplayedInitial = true;
    }

    private interface TransactionVisitor<T extends TransactionNode> {

        void applyToItem(T node, ItemStack item, int index);
    }

    private static class SingleTransactionVisitor implements TransactionVisitor<SingleTransactionNode>  {

        @Override
        public void applyToItem(SingleTransactionNode node, ItemStack item, int index) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwner(node.toUserName());
            meta.setDisplayName("§e" + node.fromUserName() + "§7 -> §e" + node.toUserName() + " §8(§e" + index + "§8)");

            List<String> lore = new ArrayList<>();
            lore.add("§7Beløb: " + AMOUNT_FORMATTER.format(node.amount()) + " emeralder");
            lore.add("§7Tidspunkt: " + TIME_FORMATTER.format(node.time()));
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
    }

    private static class GroupedTransactionVisitor implements TransactionVisitor<TransactionNode.GroupedTransactionNode> {

        @Override
        public void applyToItem(TransactionNode.GroupedTransactionNode node, ItemStack item, int index) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            switch (node.bound()) {
                case TO:
                    String toPlayer = node.nodes().stream().map(SingleTransactionNode::toUserName).findFirst().orElseThrow();

                    meta.setOwner(toPlayer);
                    meta.setDisplayName("§7Til §e" + toPlayer + " §(§e" + index + "§8)");
                    break;
                case FROM:
                    String fromPlayer = node.nodes().stream().map(SingleTransactionNode::fromUserName).findFirst().orElseThrow();

                    meta.setOwner(fromPlayer);
                    meta.setDisplayName("§7Fra §e" + fromPlayer + " §(§e" + index + "§8)");
                    break;
            }

            item.setItemMeta(meta);
        }
    }

    private void clickNewSettings(Player player) {
        this.settings.open(player);
    }

    private void clickBack(Player player) {
        new EngineGui<>(this.context.previousContext().query(), this.context.previousContext().previousContext(), this.settings)
                .open(player);
    }

    private void clickInspection(Player player, TransactionNode node) {
        EngineQuery<N> newQuery = new EngineQuery<>(this.context.query(), false);

        player.closeInventory();
        new EngineGui<>(
                newQuery.filter(QueryFilter.FilterTypes.TIME.makeFilter(d -> d.isAfter(node.getMinTime())))
                // TODO
                        .filter((QueryFilter<? super N>) QueryFilter.FilterTypes.FROM_USER.makeFilter(p -> p.equals(((SingleTransactionNode)node).toUserName()))), this.context, this.settings)
                .open(player);
    }

    private void clickUp() {
        if (scrolledDown == 0) {
            return;
        }

        scrolledDown--;
        displayNodes();
    }

    private void clickDown() {
        if (this.context.query().size() - scrolledDown * 8 < 5 * 8) {
            return;
        }

        scrolledDown++;
        displayNodes();
    }
}
