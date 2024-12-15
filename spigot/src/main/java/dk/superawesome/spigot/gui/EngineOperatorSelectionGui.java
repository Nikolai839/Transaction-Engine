package dk.superawesome.spigot.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.QueryFilter;
import dk.superawesome.core.transaction.SingleTransactionNode;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class EngineOperatorSelectionGui {

    interface Selection extends QueryFilter.Operator<SingleTransactionNode> {

        ItemStack show();
    }

    record Single(QueryFilter.FilterType<?, SingleTransactionNode> type, ItemStack item) implements Selection {

        @Override
        public Predicate<SingleTransactionNode> test(List<QueryFilter.FilterData<? super SingleTransactionNode>> filters) {
            return n -> filters.stream()
                    .filter(f -> f.type().equals(this.type))
                    .allMatch(f -> f.filter().test(n));
        }

        @Override
        public ItemStack show() {
            return ItemBuilder.from(item)
                    .lore(Component.text("§7Shift+Klik for at fjerne"))
                    .build();
        }
    }

    record Group(List<Selection> selections, AtomicReference<OperatorType> type) implements Selection {

        enum OperatorType {
            AND, OR
        }

        @Override
        public Predicate<SingleTransactionNode> test(List<QueryFilter.FilterData<? super SingleTransactionNode>> filters) {
            return switch (this.type.get()) {
                case AND -> n -> this.selections.stream().allMatch(s -> s.test(filters).test(n));
                case OR -> n -> this.selections.stream().anyMatch(s -> s.test(filters).test(n));
            };
        }

        @Override
        public ItemStack show() {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§8Shift+Klik for at fjerne"));
            if (!this.selections.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("§7Har følgende elementer:"));
                appendGroupLore(this.selections, lore, 0);
            }

            return ItemBuilder.from(Material.CHEST)
                    .name(Component.text("§6Gruppe"))
                    .lore(lore)
                    .build();
        }

        private void appendGroupLore(List<Selection> selections, List<Component> lore, int nested) {
            String def = " ".repeat(nested);
            for (Selection sel : selections) {
                if (sel instanceof Single single) {
                    lore.add(Component.text(def + "§8- §7" + getName(single.type())));
                } else if (sel instanceof Group group) {
                    lore.add(Component.text(def + "§8Gruppe:"));
                    appendGroupLore(group.selections(), lore, nested + 1);
                }
            }
        }

        private String getName(QueryFilter.FilterType<?, SingleTransactionNode> type) {
            if (type.equals(QueryFilter.FROM_USER)) {
                return "Fra spiller";
            } else if (type.equals(QueryFilter.TO_USER)) {
                return "Til spiller";
            } else if (type.equals(QueryFilter.AMOUNT)) {
                return "Beløb";
            } else if (type.equals(QueryFilter.TIME)) {
                return "Tidspunkt";
            } else if (type.equals(QueryFilter.TYPE)) {
                return "Transaktionstype";
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private final EngineSettingsGui settingsGui;
    private final Gui gui;
    private final GuiItem addGroupItem;
    private Group operator = new Group(new ArrayList<>(), new AtomicReference<>(Group.OperatorType.AND));
    private final Deque<Group> prev = new LinkedList<>();

    public EngineOperatorSelectionGui(EngineSettingsGui settingsGui) {
        this.settingsGui = settingsGui;

        this.gui = Gui.gui()
                .title(Component.text("Vælg filtreringslogik"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i = 36; i <= 44; i++) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }
        this.gui.setItem(48, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)));

        this.gui.setItem(45, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 14))
                        .name(Component.text("§cTilbage til indstillinger"))
                        .build(),
                e -> openSettings((Player) e.getWhoClicked())
        ));

        this.addGroupItem = new GuiItem(
                ItemBuilder.from(Material.NAME_TAG)
                        .name(Component.text("§6Tilføj gruppe"))
                        .build(),
                __ -> addGroup()
        );

        if (this.settingsGui.getOperator() instanceof Group group) {
            this.operator = group;
        }

        display();
    }

    private void displayOperatorTypeItem() {
        List<Component> lore = new ArrayList<>();
        if (this.operator.type().get().equals(Group.OperatorType.AND)) {
            lore.add(Component.text("§7Kræver alle filtre §8(Klik)"));
        } else {
            lore.add(Component.text("§7Kræver én af alle filtre §8(Klik)"));
        }
        this.gui.updateItem(47, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.PAPER))
                        .name(Component.text("§6Vælg filtertjek"))
                        .lore(lore)
                        .build(), __ -> changeOperatorType()
        ));
        this.gui.update();
    }

    private void changeOperatorType() {
        this.operator.type().set(this.operator.type().get().equals(Group.OperatorType.AND) ? Group.OperatorType.OR : Group.OperatorType.AND);
        displayOperatorTypeItem();
    }

    private void addGroup() {
        this.operator.selections().add(new Group(new ArrayList<>(), new AtomicReference<>(Group.OperatorType.AND)));
        display();
    }

    private boolean hasSelection(QueryFilter.FilterType<?, SingleTransactionNode> type) {
        return this.operator.selections().stream()
                .filter(s -> s instanceof Single single)
                .map(s -> (Single) s)
                .map(Single::type)
                .anyMatch(type::equals);
    }

    private void display() {
        for (int i = 49; i <= 53; i++) {
            this.gui.updateItem(i, new GuiItem(Material.AIR));
        }

        List<GuiItem> items = new ArrayList<>();
        if (!this.settingsGui.getToUserNames().isEmpty() && !hasSelection(QueryFilter.TO_USER)) {
            items.add(new GuiItem(getItemFromType(QueryFilter.TO_USER), this::addToPlayer));
        }
        if (!this.settingsGui.getFromUserNames().isEmpty() && !hasSelection(QueryFilter.FROM_USER)) {
            items.add(new GuiItem(getItemFromType(QueryFilter.FROM_USER), this::addFromPlayer));
        }
        if ((this.settingsGui.getAmountTo() != -1 || this.settingsGui.getAmountFrom() != -1) && !hasSelection(QueryFilter.AMOUNT)) {
            items.add(new GuiItem(getItemFromType(QueryFilter.AMOUNT), this::addAmount));
        }
        if ((this.settingsGui.getTimeTo() != null || this.settingsGui.getTimeFrom() != null) && !hasSelection(QueryFilter.TIME)) {
            items.add(new GuiItem(getItemFromType(QueryFilter.TIME), this::addTime));
        }
        if (this.settingsGui.getPayType() != null && !hasSelection(QueryFilter.TYPE)) {
            items.add(new GuiItem(getItemFromType(QueryFilter.TYPE), this::addType));
        }
        int j = 49;
        for (GuiItem item : items) {
            this.gui.updateItem(j++, item);
        }

        for (int i = 0; i <= 35; i++) {
            this.gui.updateItem(i, new GuiItem(Material.AIR));
        }

        int i = 0;
        for (Selection sel : this.operator.selections()) {
            this.gui.updateItem(i++, new GuiItem(sel.show(), e -> clickSelection(e, sel)));
        }

        this.gui.updateItem(this.operator.selections().size(), addGroupItem);

        displayOperatorTypeItem();
        this.gui.update();
    }

    private void clickSelection(InventoryClickEvent event, Selection sel) {
        if (event.isShiftClick()) {
            this.operator.selections().remove(sel);
            display();
            return;
        }

        if (sel instanceof Group group) {
            if (this.prev.isEmpty()) {
                this.gui.updateItem(46, new GuiItem(
                        ItemBuilder.from(Material.ARROW)
                                .name(Component.text("§6Gå tilbage"))
                                .build(),
                        __ -> clickBack()
                ));
            }

            this.prev.addFirst(this.operator);
            this.operator = group;
            display();
        }
    }

    private void openSettings(Player player) {
        List<QueryFilter.FilterType<?, SingleTransactionNode>> types = new ArrayList<>();
        if (!this.settingsGui.getToUserNames().isEmpty()) {
            types.add(QueryFilter.TO_USER);
        }
        if (!this.settingsGui.getFromUserNames().isEmpty()) {
            types.add(QueryFilter.FROM_USER);
        }
        if (this.settingsGui.getAmountTo() != -1 || this.settingsGui.getAmountFrom() != -1) {
            types.add(QueryFilter.AMOUNT);
        }
        if (this.settingsGui.getTimeTo() != null || this.settingsGui.getTimeFrom() != null) {
            types.add(QueryFilter.TIME);
        }
        if (this.settingsGui.getPayType() != null) {
            types.add(QueryFilter.TYPE);
        }

        checkTypesPresent(types, this.operator.selections());
        if (!types.isEmpty()) {
            if (!this.operator.selections().isEmpty()) {
                player.sendMessage("§cDu indstillede ikke alle de filtre du har valgt!");
                this.settingsGui.open(player);
                return;
            }

            this.operator.selections().addAll(types.stream().map(t -> new Single(t, getItemFromType(t))).toList());
        }

        this.settingsGui.setOperator(this.operator);
        this.settingsGui.open(player);
    }

    private ItemStack getItemFromType(QueryFilter.FilterType<?, SingleTransactionNode> type) {
        if (type.equals(QueryFilter.TO_USER)) {
            return ItemBuilder.from(Material.SIGN)
                    .name(Component.text("§6Til spiller"))
                    .build();
        } else if (type.equals(QueryFilter.FROM_USER)) {
            return ItemBuilder.from(Material.SIGN)
                    .name(Component.text("§6Fra spiller"))
                    .build();
        } else if (type.equals(QueryFilter.AMOUNT)) {
            return ItemBuilder.from(Material.GOLD_INGOT)
                    .name(Component.text("§6Beløb"))
                    .build();
        } else if (type.equals(QueryFilter.TIME)) {
            return ItemBuilder.from(Material.COMPASS)
                    .name(Component.text("§6Tidspunkt"))
                    .build();
        } else if (type.equals(QueryFilter.TYPE)) {
            return ItemBuilder.from(Material.GLASS_BOTTLE)
                    .name(Component.text("§6Transaktionstype"))
                    .build();
        } else {
            throw new IllegalStateException();
        }
    }

    private void checkTypesPresent(List<QueryFilter.FilterType<?, SingleTransactionNode>> types, List<Selection> selections) {
        for (Selection sel : selections) {
            if (sel instanceof Single single) {
                types.remove(single.type());
            } else if (sel instanceof Group group) {
                checkTypesPresent(types, group.selections());
            }

            if (types.isEmpty()) {
                break;
            }
        }
    }

    private void clickBack() {
        this.operator = this.prev.poll();
        if (this.prev.isEmpty()) {
            this.gui.updateItem(46, new ItemStack(Material.AIR));
        }

        display();
    }

    private void addSelection(QueryFilter.FilterType<?, SingleTransactionNode> type, InventoryClickEvent event) {
        this.operator.selections().add(new Single(type, event.getCurrentItem()));
        display();
    }

    private void addToPlayer(InventoryClickEvent event) {
        addSelection(QueryFilter.TO_USER, event);
    }

    private void addFromPlayer(InventoryClickEvent event) {
        addSelection(QueryFilter.FROM_USER, event);
    }

    private void addAmount(InventoryClickEvent event) {
        addSelection(QueryFilter.AMOUNT, event);
    }

    private void addTime(InventoryClickEvent event) {
        addSelection(QueryFilter.TIME, event);
    }

    private void addType(InventoryClickEvent event) {
        addSelection(QueryFilter.TYPE, event);
    }

    public void open(Player player) {
        this.gui.open(player);
    }
}
