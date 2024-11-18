package dk.superawesome.spigot.gui;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import dk.superawesome.core.exceptions.RequestException;
import dk.superawesome.spigot.Cache;
import dk.superawesome.spigot.DatabaseController;
import dk.superawesome.spigot.TransactionEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EngineSettingsGui {

    private static final Cache CACHE = new Cache();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Copenhagen");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);
    private static final BiFunction<Player, Gui, SignGUIAction> SIGN_CALLBACK = (player, gui) -> SignGUIAction.runSync(TransactionEngine.instance, () -> gui.open(player));

    private final Gui gui;

    private SortingMethod sortingMethod = SortingMethod.BY_TIME;
    private boolean sortHighestToLowest = true;
    private boolean traceModeEnabled;
    private boolean groupUserNamesEnabled;
    private int groupUserNamesMax = -1;
    private int groupUserNamesMaxBetween;
    private TimeUnit groupUserNamesMaxBetweenUnit;
    private boolean groupUserNamesFrom;
    private final List<String> toUserNames = new ArrayList<>();
    private final List<String> fromUserNames = new ArrayList<>();
    private double amountFrom = -1;
    private double amountTo = -1;
    private ZonedDateTime timeFrom;
    private ZonedDateTime timeTo;

    public EngineSettingsGui() {
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (Indstillinger)"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i : Arrays.asList(7, 8, 16, 17, 25, 26, 34, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52)) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }
        for (int i : Arrays.asList(9, 10, 11, 12, 13, 14, 15, 27, 28, 29, 30, 31, 32, 33, 37, 39)) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7)));
        }

        this.gui.setItem(0, new GuiItem(
                ItemBuilder.from(Material.SIGN)
                        .name(Component.text("§6Tilføj til spiller"))
                        .build(), event -> addToUser((Player) event.getWhoClicked())));

        this.gui.setItem(18, new GuiItem(
                ItemBuilder.from(Material.SIGN)
                        .name(Component.text("§6Tilføj fra spiller"))
                        .build(), event -> addFromUser((Player) event.getWhoClicked())));

        this.gui.setItem(40, new GuiItem(
                ItemBuilder.from(Material.BUCKET)
                        .name(Component.text("§6Gruppér efter"))
                        .build(), __ -> configureGrouped()));

        this.gui.setItem(35, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 14))
                        .name(Component.text("§cNulstil indstillinger"))
                        .build(), __ -> resetSettings()));

        updateItems();
    }

    public SortingMethod getSortingMethod() {
        return this.sortingMethod;
    }

    public void open(Player player) {
        this.gui.open(player);
    }

    private void changeSortingMethod(InventoryClickEvent event) {
        if (event.getClick().isRightClick()) {
            this.sortHighestToLowest = !this.sortHighestToLowest;
        } else {
            this.sortingMethod = SortingMethod.values()[(sortingMethod.ordinal() + 1) % SortingMethod.values().length];
        }

        updateSortingItem();
    }

    private void configureTraceMode() {
        this.traceModeEnabled = !this.traceModeEnabled;
        updateTraceModeItem();
    }

    private void configureGrouped() {

    }

    private void updateUsers() {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 0, (byte) SkullType.PLAYER.ordinal());

        for (int i = 0; i < 5; i++) {
            if (i < this.toUserNames.size()) {
                String toUser = this.toUserNames.get(i);
                ItemStack clone = item.clone();
                setUser(toUser, clone);

                this.gui.updateItem(i + 1, new GuiItem(clone, __ -> removeToUserName(toUser)));
            } else {
                this.gui.updateItem(i + 1, new GuiItem(Material.AIR));
            }
        }

        for (int i = 0; i < 5; i++) {
            if (i < this.fromUserNames.size()) {
                String fromUser = this.fromUserNames.get(i);
                ItemStack clone = item.clone();
                setUser(fromUser, clone);

                this.gui.updateItem(i + 19, new GuiItem(clone, __ -> removeFromUserName(fromUser)));
            } else {
                this.gui.updateItem(i + 19, new GuiItem(Material.AIR));
            }
        }

        updateExecuteItem();
    }

    private void setUser(String user, ItemStack item) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(user);
        meta.setDisplayName("§e" + user + " §8(Tryk for at fjerne)");
        item.setItemMeta(meta);
    }

    private void removeFromUserName(String user) {
        if (this.fromUserNames.contains(user)) {
            this.fromUserNames.remove(user);
            updateUsers();
        }
    }

    private void removeToUserName(String user) {
        if (this.toUserNames.contains(user)) {
            this.toUserNames.remove(user);
            updateUsers();
        }
    }

    private boolean isValidUser(String user) {
        return USERNAME.matcher(user).matches();
    }

    private void addToUser(Player player) {
        SignGUI.builder()
                .setLine(0, "Vælg til spiller")
                .setHandler((__, result) -> {
                    boolean added = false;
                    for (String line : Arrays.copyOfRange(result.getLines(), 1, 4)) {
                        if (!line.isEmpty() && isValidUser(line) && this.toUserNames.stream().noneMatch(line::equalsIgnoreCase)) {
                            this.toUserNames.add(line);
                            added = true;
                        }
                    }

                    if (!added) {
                        player.sendMessage("§cIngen gyldig spiller valgt!");
                        return Collections.singletonList(SignGUIAction.run(() -> addToUser(player)));
                    }

                    updateUsers();

                    return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                })
                .build()
                .open(player);
    }

    private void addFromUser(Player player) {
        SignGUI.builder()
                .setLine(0, "Vælg fra spiller")
                .setHandler((__, result) -> {
                    boolean added = false;
                    for (String line : Arrays.copyOfRange(result.getLines(), 1, 4)) {
                        if (!line.isEmpty() && isValidUser(line) && this.fromUserNames.stream().noneMatch(line::equalsIgnoreCase)) {
                            this.fromUserNames.add(line);
                            added = true;
                        }
                    }

                    if (!added) {
                        player.sendMessage("§cIngen gyldig spiller valgt!");
                        return Collections.singletonList(SignGUIAction.run(() -> addFromUser(player)));
                    }

                    updateUsers();

                    return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                })
                .build()
                .open(player);
    }

    private void configureAmountRange(Player player) {
        SignGUI.builder()
                .setLine(0, "Vælg fra")
                .setLine(1, this.amountFrom != -1 ? String.valueOf(this.amountFrom) : "")
                .setLine(2, "Vælg til")
                .setLine(3, this.amountTo != -1 ? String.valueOf(this.amountTo) : "")
                .setHandler((__, result) -> {
                    String fromString = result.getLine(1);
                    String toString = result.getLine(3);

                    if (fromString.isEmpty() && toString.isEmpty()) {
                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                    }

                    try {
                        double from = -1;
                        double to = -1;

                        if (!fromString.isEmpty()) {
                            from = Double.parseDouble(fromString);
                            if (from < 0) {
                                throw new NumberFormatException();
                            }
                        }
                        if (!toString.isEmpty()) {
                            to = Double.parseDouble(toString);
                            if (to < 0) {
                                throw new NumberFormatException();
                            }
                        }

                        if (from != -1) {
                            if (from > to && to != -1) {
                                throw new NumberFormatException();
                            }
                            this.amountFrom = from;
                        }

                        if (to != -1) {
                            if (to < from) {
                                throw new NumberFormatException();
                            }
                            this.amountTo = to;
                        }

                        updateAmountItem();

                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));

                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cUgyldig antal valgt!");
                        return Collections.singletonList(SignGUIAction.run(() -> configureAmountRange(player)));
                    }
                })
                .build()
                .open(player);
    }

    private String getTimeString(ZonedDateTime time) {
        return time.getDayOfMonth() + "/" + time.getMonthValue() + "/" + time.getYear() + " " + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond();
    }

    private void configureTimeRange(Player player) {
        SignGUI.builder()
                .setLine(0, "Vælg fra")
                .setLine(1, Optional.ofNullable(this.timeFrom).map(this::getTimeString).orElse(""))
                .setLine(2, "Vælg til")
                .setLine(3, Optional.ofNullable(this.timeTo).map(this::getTimeString).orElse(""))

                .setHandler((__, result) -> {
                    String fromString = result.getLine(1);
                    String toString = result.getLine(3);

                    if (fromString.isEmpty() && toString.isEmpty()) {
                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                    }

                    try {
                        LocalDateTime from = null;
                        LocalDateTime to = null;

                        if (!fromString.isEmpty()) {
                            from = parseString(fromString);
                        }
                        if (!toString.isEmpty()) {
                            to = parseString(toString);
                        }

                        if (to != null) {
                            ZonedDateTime zonedTo = to.atZone(ZONE_ID);
                            if (zonedTo.toInstant().isAfter(Instant.now())) {
                                throw new InvalidDateException();
                            }
                            this.timeTo = zonedTo;
                        }
                        if (from != null) {
                            ZonedDateTime zonedFrom = from.atZone(ZONE_ID);
                            if (zonedFrom.toInstant().isAfter(Instant.now())) {
                                throw new InvalidDateException();
                            }
                            this.timeFrom = zonedFrom;
                        }

                        updateTimeItem();

                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));

                    } catch (InvalidDateException ex) {
                        player.sendMessage("§cUgyldig dato valgt!");
                        return Collections.singletonList(SignGUIAction.run(() -> configureTimeRange(player)));
                    }
                })
                .build()
                .open(player);
    }

    private static class InvalidDateException extends Exception {

    }

    private static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }

    private LocalDateTime parseString(String dateString) throws InvalidDateException {
        String[] daysMonthsYears = Stream.concat(Arrays.stream(dateString.split("/")), Arrays.stream(dateString.split("-")))
                .filter(Predicate.not(dateString::equals))
                .map(String::trim)
                .map(s -> s.codePoints().takeWhile(i -> i != ' ').collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append))
                .map(StringBuilder::toString)
                .toArray(String[]::new);
        String[] timeOfDay = dateString.split(":");
        timeOfDay[0] = reverse(
                reverse(timeOfDay[0]).codePoints()
                        .takeWhile(i -> i != ' ')
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString());

        if (daysMonthsYears.length != 3) {
            Bukkit.broadcastMessage(Arrays.toString(daysMonthsYears) + " ");
            throw new InvalidDateException();
        }
        if (timeOfDay.length > 3) {
            throw new InvalidDateException();
        }

        LocalDateTime time;
        try {
            int day = Integer.parseInt(daysMonthsYears[0]);
            if (day > 31 || day < 1) {
                throw new InvalidDateException();
            }

            int month = Integer.parseInt(daysMonthsYears[1]);
            if (month > 12 || month < 1) {
                throw new InvalidDateException();
            }

            int year = Integer.parseInt(daysMonthsYears[2]);
            if (String.valueOf(year).length() == 2) {
                year = 2000 + year; // fix year (e.g. 23 -> 2023)
            } else if (year < 1970) { // 0 unix year
                throw new InvalidDateException();
            }

            try {
                LocalDate date = LocalDate.of(year, month, day);
                if (timeOfDay.length != 1) {
                    int hour = Integer.parseInt(timeOfDay[0]);
                    if (hour > 23) {
                        throw new InvalidDateException();
                    }

                    int minute = Integer.parseInt(timeOfDay[1]);
                    if (minute > 59) {
                        throw new InvalidDateException();
                    }

                    int second = 0;
                    if (timeOfDay.length > 2) {
                        second = Integer.parseInt(timeOfDay[2]);
                    }

                    time = date.atTime(hour, minute, second);
                } else {
                    time = LocalDateTime.of(date, LocalTime.MIN);
                }
            } catch (DateTimeException ex) {
                throw new InvalidDateException();
            }

        } catch (NumberFormatException ex) {
            throw new InvalidDateException();
        }

        return time;
    }

    private void resetSettings() {
        this.sortingMethod = SortingMethod.BY_TIME;
        this.sortHighestToLowest = true;
        this.traceModeEnabled = false;
        this.groupUserNamesEnabled = false;
        this.groupUserNamesMax = -1;
        this.groupUserNamesFrom = false;
        this.groupUserNamesMaxBetween = -1;
        this.groupUserNamesMaxBetweenUnit = null;
        this.toUserNames.clear();
        this.fromUserNames.clear();
        this.amountFrom = -1;
        this.amountTo = -1;
        this.timeFrom = null;
        this.timeTo = null;

        updateItems();
    }

    private void updateItems() {
        updateUsers();
        updateTraceModeItem();
        updateSortingItem();
        updateAmountItem();
        updateTimeItem();

        this.gui.update();
    }

    private void updateExecuteItem() {
        List<Component> executeItemLore = new ArrayList<>();
        executeItemLore.add(Component.text("§8Indstillinger:"));

        List<Component> filters = new ArrayList<>();
        if (!this.fromUserNames.isEmpty()) {
            filters.add(Component.text("§7Fra " + this.fromUserNames.toString().replaceAll("\\[]", "")));
        }
        if (!this.toUserNames.isEmpty()) {
            filters.add(Component.text("§7Til " + this.toUserNames.toString().replaceAll("\\[]", "")));
        }
        if (this.timeFrom != null) {
            filters.add(Component.text("§7Fra " + TIME_FORMATTER.format(this.timeFrom)));
        }
        if (this.timeTo != null) {
            filters.add(Component.text("§7Til " + TIME_FORMATTER.format(this.timeTo)));
        }
        if (this.amountFrom != -1) {
            filters.add(Component.text("§7Fra " + this.amountFrom + " emeralder"));
        }
        if (this.amountTo != -1) {
            filters.add(Component.text("§7Til " + this.amountTo + " emeralder"));
        }

        executeItemLore.add(Component.empty());
        if (filters.isEmpty()) {
            executeItemLore.add(Component.text("§7Ingen filtre"));
        } else {
            executeItemLore.add(Component.text("§8Filtre:"));
            executeItemLore.addAll(filters);
        }

        executeItemLore.add(Component.empty());
        if (this.groupUserNamesEnabled) {
            if (this.groupUserNamesFrom) {
                executeItemLore.add(Component.text("§7Gruppere fra spillere"));
            } else {
                executeItemLore.add(Component.text("§7Gruppere til spillere"));
            }

            if (this.groupUserNamesMax != -1) {
                executeItemLore.add(Component.text("§7Højest antal: " + this.groupUserNamesMax));
            }

            if (this.groupUserNamesMaxBetween != -1) {
                executeItemLore.add(Component.text("§7Højest tidsforskel: " + this.groupUserNamesMaxBetween + " " + this.groupUserNamesMaxBetweenUnit.toString().toLowerCase()));
            }
        }

        if (this.traceModeEnabled) {
            executeItemLore.add(Component.text("§7Sporingstilstand slået til"));
        }

        executeItemLore.add(Component.text("§7Sorteres efter " + this.sortingMethod.toString().toLowerCase().replace("_", "-")));
        if (this.sortHighestToLowest) {
            executeItemLore.add(Component.text("§7Sorteres højest til lavest"));
        } else {
            executeItemLore.add(Component.text("§7Sorteres lavest til højest"));
        }

        this.gui.updateItem(53, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 5))
                        .name(Component.text("§aUdfør søgning"))
                        .lore(executeItemLore)
                        .build(), event -> openEngineGui((Player) event.getWhoClicked())));
    }

    private void updateTimeItem() {
        List<Component> timeItemLore = new ArrayList<>();
        if (this.timeFrom == null && this.timeTo == null) {
            timeItemLore.add(Component.text("§7Alle tidspunkter"));
        } else {
            if (this.timeFrom != null) {
                timeItemLore.add(Component.text("§7Fra " + TIME_FORMATTER.format(this.timeFrom)));
            }
            if (this.timeTo != null) {
                timeItemLore.add(Component.text("§7Fra " + TIME_FORMATTER.format(this.timeTo)));
            }
        }

        this.gui.updateItem(38, new GuiItem(
                ItemBuilder.from(Material.COMPASS)
                        .name(Component.text("§6Indstil tidspunkter"))
                        .lore(timeItemLore)
                        .build(), event -> configureTimeRange((Player) event.getWhoClicked())));

        updateExecuteItem();
    }

    private void updateAmountItem() {
        List<Component> amountItemLore = new ArrayList<>();
        if (this.amountFrom == -1 && this.amountTo == -1) {
            amountItemLore.add(Component.text("§7Alle beløber"));
        } else {
            if (this.amountFrom != 1) {
                amountItemLore.add(Component.text("§7Fra " + this.amountFrom + " emeralder"));
            }
            if (this.amountTo != -1) {
                amountItemLore.add(Component.text("§7Fra " + this.amountTo + " emeralder"));
            }
        }

        this.gui.updateItem(36, new GuiItem(
                ItemBuilder.from(Material.GOLD_INGOT)
                        .name(Component.text("§6Indstil beløber"))
                        .lore(amountItemLore)
                        .build(), event -> configureAmountRange((Player) event.getWhoClicked())));

        updateExecuteItem();
    }

    private void updateTraceModeItem() {
        this.gui.updateItem(41, new GuiItem(ItemBuilder.from(Material.LEATHER_BOOTS)
                .name(Component.text("§6Sporingstilstand"))
                .lore(Component.text((this.traceModeEnabled ? "§7Slået til" : "§7Slået fra") + " §8(Klik)"))
                .glow(this.traceModeEnabled)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build(), __ -> configureTraceMode()));

        updateExecuteItem();
    }

    private void updateSortingItem() {
        List<Component> sortingItemLore = new ArrayList<>();
        if (this.sortHighestToLowest) {
            sortingItemLore.add(Component.text("§7Højest til lavest §8(Højreklik)"));
        } else {
            sortingItemLore.add(Component.text("§7Lavest til højest §8(Højreklik)"));
        }
        sortingItemLore.add(Component.empty());

        sortingItemLore.add(Component.text("§7Sorteres efter: §8(Venstreklik)"));
        for (SortingMethod method : SortingMethod.values()) {
            String colour = this.sortingMethod == method ? "§e" : "§8";
            sortingItemLore.add(Component.text(colour + " - " + method.name().toLowerCase().replace("_", "-")));
        }

        this.gui.updateItem(42, new GuiItem(ItemBuilder.from(Material.BOOK_AND_QUILL)
                .name(Component.text("§6Sortér efter"))
                .lore(sortingItemLore)
                .glow(this.sortHighestToLowest)
                .build(), this::changeSortingMethod));

        updateExecuteItem();
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

    @SuppressWarnings("unchecked")
    private <T extends TransactionNode> void openEngineGuiAsync(Player player, Consumer<BukkitRunnable> callback) {
        try {
            DatabaseController controller = TransactionEngine.instance.getDatabaseController();
            TransactionRequestBuilder builder = EngineRequest.Builder.makeRequest(TransactionRequestBuilder.class, CACHE, TransactionEngine.instance.getSettings(), controller, controller.getRequester());

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

            EngineQuery<SingleTransactionNode> query = Engine.queryFromCache(builder.build());
            if (this.traceModeEnabled) {
                // TODO
            }

            Node.Collection collection = Node.Collection.SINGLE;
            EngineQuery<T> finalQuery;
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

                List<PostQueryTransformer.GroupBy.GroupOperator<SingleTransactionNode>> operators = new ArrayList<>();
                if (this.groupUserNamesMax != -1) {
                    operators.add(PostQueryTransformer.GroupBy.GroupOperator.max(this.groupUserNamesMax));
                }
                if (this.groupUserNamesMaxBetween != -1) {
                    operators.add(PostQueryTransformer.GroupBy.GroupOperator.maxBetween(SingleTransactionNode::time, this.groupUserNamesMaxBetween, this.groupUserNamesMaxBetweenUnit));
                }

                finalQuery = (EngineQuery<T>) query.transform(
                        PostQueryTransformer.GroupBy.<SingleTransactionNode, TransactionNode.GroupedTransactionNode>groupBy(
                                func, Object::equals,
                                PostQueryTransformer.GroupBy.GroupOperator.mix(operators),
                                nodes -> new TransactionNode.GroupedTransactionNode(nodes, bound)));
            } else {
                finalQuery = (EngineQuery<T>) query;
            }

            finalQuery = Engine.doTransformation(collection, this.sortingMethod, finalQuery);
            if (this.sortHighestToLowest) {
                finalQuery.transform(PostQueryTransformer.reversed());
            }

            EngineQuery<? extends TransactionNode> queryBuffer = finalQuery;
            callback.accept(new BukkitRunnable() {

                @Override
                public void run() {
                    new EngineGui<>(queryBuffer, EngineSettingsGui.this).open(player);
                }
            });

        } catch (RequestException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Faild to query", ex);
        }
    }
}
