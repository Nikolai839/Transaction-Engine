package dk.superawesome.spigot.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.TransactionNode;
import dk.superawesome.core.transaction.TransactionRequestBuilder;
import dk.superawesome.spigot.TransactionEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

public class EngineGui<N extends TransactionNode> {

    private static final DecimalFormat EMERALD_FORMATTER = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.GERMANY));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Gui gui;
    private final QueryContext<N, ? extends TransactionNode> context;
    private final EngineSettingsGui settings;
    private TransactionVisitor<N> visitor;

    private boolean hasDisplayedInitial;
    private int scrolledDown;

    private record QueryContext<CN extends TransactionNode, FN extends TransactionNode>(QueryContext<FN, ? extends TransactionNode> previousContext, EngineQuery<CN> query) {

    }

    public EngineGui(EngineQuery<N> query, EngineSettingsGui settings) {
        this(query, null, settings);
    }

    private EngineGui(EngineQuery<N> query, QueryContext<?, ?> previousContext, EngineSettingsGui settings) {
        this.context = new QueryContext<>(previousContext, query);
        this.settings = settings;
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (" + query.size() + (query.size() < query.initialNodes().size() ? "/" + query.initialNodes().size() : "") + ")"))
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

    public boolean open(Player player) {
        if (!this.context.query().isEmpty()) {
            this.gui.open(player);
            return true;
        } else {
            player.sendMessage("§cDenne transaktionsforespørgsel er tom!");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private TransactionVisitor<N> getVisitor(TransactionNode node) {
        if (this.visitor != null) {
            return this.visitor;
        }

        return switch (node.getCollection()) {
            case SINGLE ->
                    this.visitor = (TransactionVisitor<N>) new SingleTransactionVisitor((QueryContext<SingleTransactionNode, ?>) this.context, this.settings);
            case GROUPED ->
                    this.visitor = (TransactionVisitor<N>) new GroupedTransactionVisitor((QueryContext<TransactionNode.GroupedTransactionNode, SingleTransactionNode>) this.context, this.settings);
            case GROUP_GROUPED ->
                    this.visitor = (TransactionVisitor<N>) new GroupGroupedTransactionVisitor((QueryContext<TransactionNode.GroupedBothWayTransactionNode, SingleTransactionNode>) this.context, this.settings);
        };
    }

    private void displayNodes() {
        if (hasDisplayedInitial && !this.context.query().isEmpty()) {
            // clear previous items
            for (int i = 1; i < 9; i++) {
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
                this.gui.setItem(slot, new GuiItem(item, event -> this.visitor.clickInspection((Player) event.getWhoClicked(), this, node)));
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

    private void clickNewSettings(Player player) {
        this.settings.open(player);
    }

    @SuppressWarnings("unchecked")
    private <CN extends TransactionNode> void clickBack(Player player) {
        new EngineGui<>((EngineQuery<CN>) this.context.previousContext().query(), this.context.previousContext().previousContext(), this.settings)
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

    private interface TransactionVisitor<T extends TransactionNode> {

        void applyToItem(T node, ItemStack item, int index);

        void clickInspection(Player player, EngineGui<T> gui, T node);
    }

    private record SingleTransactionVisitor(QueryContext<SingleTransactionNode, ?> context, EngineSettingsGui settings) implements TransactionVisitor<SingleTransactionNode> {

        @Override
        public void applyToItem(SingleTransactionNode node, ItemStack item, int index) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwner(node.toUserName());
            meta.setDisplayName("§e" + node.fromUserName() + "§7 -> §e" + node.toUserName() + " §8(§e" + index + "§8) (Klik)");

            List<String> lore = new ArrayList<>();
            lore.add("§8Ved inspektion, ser du alle transaktioner");
            lore.add("§8denne spiller har overført efter datoen.");
            lore.add("§8§o(Med de forrige filtre valgt)");
            lore.add("");
            lore.add("§7Beløb: " + EMERALD_FORMATTER.format(node.amount()) + " emeralder");
            lore.add("§7Tidspunkt: " + TIME_FORMATTER.format(node.time()));
            lore.add("§7Transaktionstype: " + node.type().toString().toLowerCase());

            if (node.extra() != null) {
                lore.add("§7Ekstra: " + node.extra());
            }

            if (node.isTraced()) {
                lore.add("");
                lore.add("§8Sporet efter:");

                SingleTransactionNode.Traced traced = (SingleTransactionNode.Traced) node;
                if (!node.fromUserName().equals(TransactionNode.CONSOLE)) {
                    lore.add("§7" + node.fromUserName() + ": " + (traced.fromUserTrace() > 0 ? "+" : "") + EMERALD_FORMATTER.format(traced.fromUserTrace()) + " emeralder");
                }
                if (!node.toUserName().equals(TransactionNode.CONSOLE)) {
                    lore.add("§7" + node.toUserName() + ": " + (traced.toUserTrace() > 0 ? "+" : "") + EMERALD_FORMATTER.format(traced.toUserTrace()) + " emeralder");
                }
            }

            List<String> newBalanceLore = new ArrayList<>();
            if (!node.fromUserName().equals(TransactionNode.CONSOLE) && node.fromUserPreBalance() != -1) {
                double fromPlayerNewBalance = node.fromUserPreBalance() - node.amount();
                newBalanceLore.add("§7" + node.fromUserName() + ": " + EMERALD_FORMATTER.format(fromPlayerNewBalance) + " emeralder");
            }

            if (!node.toUserName().equals(TransactionNode.CONSOLE) && node.toUserPreBalance() != -1) {
                double toPlayerNewBalance = node.toUserPreBalance() + node.amount();
                newBalanceLore.add("§7" + node.toUserName() + ": " + EMERALD_FORMATTER.format(toPlayerNewBalance) + " emeralder");
            }

            if (!newBalanceLore.isEmpty()) {
                lore.add("");
                lore.add("§8Nye balancer:");
                lore.addAll(newBalanceLore);
            }

            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        @Override
        public void clickInspection(Player player, EngineGui<SingleTransactionNode> gui, SingleTransactionNode node) {
            EngineLoadingGui loadingGui = new EngineLoadingGui();
            loadingGui.open(player);
            Bukkit.getScheduler().runTaskAsynchronously(TransactionEngine.instance, () -> {
                EngineQuery<SingleTransactionNode> buffer = TransactionRequestBuilder.wrap(new EngineQuery<>(this.context.query(), false))
                        .from(node.time())
                        .from(node.toUserName())
                        .build();

                TransactionRequestBuilder<EngineRequest.QueryWrapperBuilder<SingleTransactionNode>, EngineQuery<SingleTransactionNode>> newQueryBuilder = TransactionRequestBuilder.wrap(new EngineQuery<>(buffer.nodes()));
                if (this.settings.getAmountFrom() != -1) {
                    newQueryBuilder.from(this.settings.getAmountFrom());
                }

                if (this.settings.getAmountTo() != -1) {
                    newQueryBuilder.to(this.settings.getAmountTo());
                }

                if (this.settings.getTimeTo() != null) {
                    newQueryBuilder.to(this.settings.getTimeTo());
                }

                if (!this.settings.getIgnorePayTypes().isEmpty()) {
                    newQueryBuilder.isNot(this.settings.getIgnorePayTypes().toArray(TransactionNode.PayType[]::new));
                } else {
                    List<TransactionNode.PayType> types = new ArrayList<>(this.settings.getExtraPayTypes());
                    TransactionNode.PayType current = this.settings.getPayType();
                    if (current != null && !types.contains(current)) {
                        types.add(current);
                    }

                    if (!types.isEmpty()) {
                        newQueryBuilder.is(types.toArray(TransactionNode.PayType[]::new));
                    }
                }

                newQueryBuilder.setOperator(this.settings.getOperator());

                EngineQuery<SingleTransactionNode> query = Engine.sort(Node.Collection.SINGLE, this.settings.getSortingMethod(), newQueryBuilder.build());
                if (this.settings.isSortHighestToLowest()) {
                    query = query.transform(PostQueryTransformer.reversed());
                }

                if (!loadingGui.isTaskCancelled()) {
                    EngineQuery<SingleTransactionNode> finalQuery = query;
                    Bukkit.getScheduler().runTask(TransactionEngine.instance, () -> {
                        if (!new EngineGui<>(finalQuery, this.context, this.settings).open(player)) {
                            gui.open(player);
                        }
                    });
                }
            });
        }
    }

    private record GroupGroupedTransactionVisitor(QueryContext<TransactionNode.GroupedBothWayTransactionNode, SingleTransactionNode> context, EngineSettingsGui settings) implements TransactionVisitor<TransactionNode.GroupedBothWayTransactionNode> {

        @Override
        public void applyToItem(TransactionNode.GroupedBothWayTransactionNode group, ItemStack item, int index) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwner(group.username());
            meta.setDisplayName("§7Fra/Til §e" + group.username() + " §8(§e" + index + "§8) (Klik)");

            List<String> lore = new ArrayList<>();

            Optional<SingleTransactionNode.Target> oldestOptional = group.getOldestTransaction();
            oldestOptional.ifPresent(target -> lore.add("§8Siden " + TIME_FORMATTER.format(target.node().time())));

            if (Math.abs(group.getSum()) == Math.abs(group.getAmount())) {
                lore.add("§7" + (group.getSum() > 0 ? "+" : "") + EMERALD_FORMATTER.format(group.getSum()) + " emeralder i alt");
            } else {
                lore.add("§7" + EMERALD_FORMATTER.format(group.getAmount()) + " emeralder i alt");
                lore.add("§7" + (group.getSum() > 0 ? "+" : "") + EMERALD_FORMATTER.format(group.getSum()) + " summeret emeralder i alt");
            }
            lore.add("§7" + group.combine().size() + " transaktioner i alt");

            trace:
            {
                if (group.isTraced()) {
                    Optional<SingleTransactionNode> oldestTo = group.getOldestTransaction(TransactionNode.GroupedTransactionNode.Bound.TO);
                    Optional<SingleTransactionNode> oldestFrom = group.getOldestTransaction(TransactionNode.GroupedTransactionNode.Bound.FROM);

                    SingleTransactionNode oldest;
                    TransactionNode.GroupedTransactionNode.Bound bound;
                    if (oldestTo.isEmpty() && oldestFrom.isEmpty()) {
                        break trace;
                    } else if (oldestTo.isPresent() && oldestFrom.isEmpty()) {
                        oldest = oldestTo.get();
                        bound = TransactionNode.GroupedTransactionNode.Bound.TO;
                    } else if (oldestTo.isEmpty()) {
                        oldest = oldestFrom.get();
                        bound = TransactionNode.GroupedTransactionNode.Bound.FROM;
                    } else {
                        int compared = oldestTo.get().compareTo(oldestFrom.get());
                        if (compared < 0) {
                            oldest = oldestTo.get();
                            bound = TransactionNode.GroupedTransactionNode.Bound.TO;
                        } else {
                            oldest = oldestFrom.get();
                            bound = TransactionNode.GroupedTransactionNode.Bound.FROM;
                        }
                    }

                    SingleTransactionNode.Traced tracedNode = SingleTransactionNode.Traced.cast(oldest);
                    double traced = switch (bound) {
                        case TO -> tracedNode.toUserTrace();
                        case FROM -> tracedNode.fromUserTrace();
                    };
                    traced += TransactionNode.GroupedBothWayTransactionNode.evaluate(new SingleTransactionNode.Target(bound, oldest));
                    traced -= group.getSum();

                    lore.add("");
                    lore.add("§8Sporet yderligere:");
                    lore.add("§7" + (traced > 0 ? "+" : "") + EMERALD_FORMATTER.format(traced) + " emeralder");
                }
            }

            Optional<SingleTransactionNode> highestOptionalTo = group.getHighestTransaction(TransactionNode.GroupedTransactionNode.Bound.TO);
            Optional<SingleTransactionNode> highestOptionalFrom = group.getHighestTransaction(TransactionNode.GroupedTransactionNode.Bound.FROM);
            if (highestOptionalTo.isPresent() || highestOptionalFrom.isPresent()) {
                lore.add("");
                lore.add("§8Højeste transaktioner:");
                if (highestOptionalTo.isPresent()) {
                    SingleTransactionNode highestTo = highestOptionalTo.get();
                    lore.add("  §8Til " + group.username() + ":");
                    lore.add("  §7Fra " + highestTo.fromUserName() + ": " + EMERALD_FORMATTER.format(highestTo.amount()) + " emeralder");
                    lore.add("  §7Overført d. " + TIME_FORMATTER.format(highestTo.time()));
                }
                if (highestOptionalFrom.isPresent()) {
                    if (highestOptionalTo.isPresent()) {
                        lore.add("");
                    }
                    SingleTransactionNode highestFrom = highestOptionalFrom.get();
                    lore.add("  §8Fra " + group.username() + ":");
                    lore.add("  §7Til " + highestFrom.toUserName() + ": " + EMERALD_FORMATTER.format(highestFrom.amount()) + " emeralder");
                    lore.add("  §7Overført d. " + TIME_FORMATTER.format(highestFrom.time()));
                }
            }

            Optional<SingleTransactionNode.Target> latestOptional = group.getLatestTransaction();
            if (latestOptional.isPresent()) {
                SingleTransactionNode.Target latest = latestOptional.get();
                TransactionNode.GroupedTransactionNode.Bound bound = latest.bound();
                SingleTransactionNode node = latest.node();

                lore.add("");
                lore.add("§8Seneste transaktion:");
                lore.add("§7" + (bound.equals(TransactionNode.GroupedTransactionNode.Bound.FROM) ? "Til" : "Fra") + " " + (bound.equals(TransactionNode.GroupedTransactionNode.Bound.FROM) ? node.toUserName() : node.fromUserName()) + ": " + EMERALD_FORMATTER.format(node.amount()) + " emeralder");
                lore.add("§7Overført d. " + TIME_FORMATTER.format(node.time()));
            }

            if (oldestOptional.isPresent()) {
                SingleTransactionNode.Target oldest = oldestOptional.get();
                TransactionNode.GroupedTransactionNode.Bound bound = oldest.bound();
                SingleTransactionNode node = oldest.node();

                lore.add("");
                lore.add("§8Ældste transaktion:");
                lore.add("§7" + (bound.equals(TransactionNode.GroupedTransactionNode.Bound.FROM) ? "Til" : "Fra") + " " + (bound.equals(TransactionNode.GroupedTransactionNode.Bound.FROM) ? node.toUserName() : node.fromUserName()) + ": " + EMERALD_FORMATTER.format(node.amount()) + " emeralder");
                lore.add("§7Overført d. " + TIME_FORMATTER.format(node.time()));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        @Override
        public void clickInspection(Player player, EngineGui<TransactionNode.GroupedBothWayTransactionNode> gui, TransactionNode.GroupedBothWayTransactionNode node) {
            clickGroupedInspection(this.context, this.settings, player, gui, node);
        }
    }

    private static <N extends TransactionNode, GN extends GroupedNode<N> & TransactionNode> void clickGroupedInspection(EngineGui.QueryContext<GN, N> context, EngineSettingsGui settings, Player player, EngineGui<GN> gui, GN group) {
        EngineLoadingGui loadingGui = new EngineLoadingGui();
        loadingGui.open(player);
        Bukkit.getScheduler().runTaskAsynchronously(TransactionEngine.instance, () -> {
            EngineQuery<N> query = new EngineQuery<>(group.nodes(), context.query().initialNodes());

            query = Engine.sort(Node.Collection.SINGLE, settings.getSortingMethod(), query);
            if (settings.isSortHighestToLowest()) {
                query = query.transform(PostQueryTransformer.reversed());
            }

            if (!loadingGui.isTaskCancelled()) {
                EngineQuery<N> finalQuery = query;
                Bukkit.getScheduler().runTask(TransactionEngine.instance, () -> {
                    if (!new EngineGui<>(finalQuery, context, settings).open(player)) {
                        gui.open(player);
                    }
                });
            }
        });
    }

    private record GroupedTransactionVisitor(QueryContext<TransactionNode.GroupedTransactionNode, SingleTransactionNode> context, EngineSettingsGui settings) implements TransactionVisitor<TransactionNode.GroupedTransactionNode> {

        @Override
        public void applyToItem(TransactionNode.GroupedTransactionNode group, ItemStack item, int index) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            String other;
            Function<SingleTransactionNode, String> targetFunction;
            switch (group.bound()) {
                case TO:
                    targetFunction = SingleTransactionNode::fromUserName;
                    String toPlayer = group.nodes().stream()
                            .map(SingleTransactionNode::toUserName)
                            .findFirst()
                            .orElseThrow(); // no inspection

                    meta.setOwner(toPlayer);
                    other = "Fra";
                    meta.setDisplayName("§7Til §e" + toPlayer + " §8(§e" + index + "§8) (Klik for inspektion)");
                    break;
                case FROM:
                    targetFunction = SingleTransactionNode::toUserName;
                    String fromPlayer = group.nodes().stream()
                            .map(SingleTransactionNode::fromUserName)
                            .findFirst()
                            .orElseThrow(); // no inspection

                    meta.setOwner(fromPlayer);
                    other = "Til";
                    meta.setDisplayName("§7Fra §e" + fromPlayer + " §8(§e" + index + "§8) (Klik for inspektion)");
                    break;
                default:
                    throw new IllegalStateException();
            }

            List<String> lore = new ArrayList<>();

            Optional<SingleTransactionNode> oldestOptional = group.getOldestTransaction();
            oldestOptional.ifPresent(target -> lore.add("§8Siden " + TIME_FORMATTER.format(target.node().time())));

            lore.add("§7" + EMERALD_FORMATTER.format(group.getAmount()) + " emeralder i alt");
            lore.add("§7" + group.size() + " transaktioner i alt");

            Optional<SingleTransactionNode> highestOptional = group.getHighestTransaction();
            if (highestOptional.isPresent()) {
                SingleTransactionNode highest = highestOptional.get();
                lore.add("");
                lore.add("§8Højeste transaktion:");
                lore.add("§7" + other + " " + targetFunction.apply(highest) + ": " + EMERALD_FORMATTER.format(highest.amount()) + " emeralder");
                lore.add("§7Overført d. " + TIME_FORMATTER.format(highest.time()));
            }

            Optional<SingleTransactionNode> latestOptional = group.getLatestTransaction();
            if (latestOptional.isPresent()) {
                SingleTransactionNode latest = latestOptional.get();
                lore.add("");
                lore.add("§8Seneste transaktion:");
                lore.add("§7" + other + " " + targetFunction.apply(latest) + ": " + EMERALD_FORMATTER.format(latest.amount()) + " emeralder");
                lore.add("§7Overført d. " + TIME_FORMATTER.format(latest.time()));
            }

            if (oldestOptional.isPresent()) {
                SingleTransactionNode oldest = oldestOptional.get();
                lore.add("");
                lore.add("§8Ældste transaktion:");
                lore.add("§7" + other + " " + targetFunction.apply(oldest) + ": " + EMERALD_FORMATTER.format(oldest.amount()) + " emeralder");
                lore.add("§7Overført d. " + TIME_FORMATTER.format(oldest.time()));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        @Override
        public void clickInspection(Player player, EngineGui<TransactionNode.GroupedTransactionNode> gui, TransactionNode.GroupedTransactionNode node) {
            clickGroupedInspection(this.context, this.settings, player, gui, node);
        }
    }
}
