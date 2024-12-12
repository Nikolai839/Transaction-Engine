package dk.superawesome.spigot.gui;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dk.superawesome.core.*;
import dk.superawesome.core.exceptions.RequestException;
import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.SortingMethod;
import dk.superawesome.core.transaction.TransactionNode;
import dk.superawesome.core.transaction.TransactionRequestBuilder;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EngineSettingsGui {

    public static final Cache CACHE = new Cache();

    public static void loadToCache() {
        Bukkit.getScheduler().runTaskAsynchronously(TransactionEngine.instance, () -> {
            try {
                Engine.query(TransactionRequestBuilder.builder(CACHE, TransactionEngine.instance.getSettings(), TransactionEngine.instance.getDatabaseController(), TransactionEngine.instance.getDatabaseController().getRequester())
                        .build());
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to query", ex);
            }
        });
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Copenhagen");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);
    private static final BiFunction<Player, Gui, SignGUIAction> SIGN_CALLBACK = (player, gui) -> SignGUIAction.runSync(TransactionEngine.instance, () -> gui.open(player));
    private static final Map<TimeUnit, String> UNIT_TO_IDENTIFIER = new HashMap<>(){{
        put (TimeUnit.DAYS, "d");
        put (TimeUnit.HOURS, "h");
        put (TimeUnit.MINUTES, "m");
        put (TimeUnit.SECONDS, "s");
    }};

    private final Gui gui;

    private final List<TransactionNode.PayType> extraTypes = new ArrayList<>();
    private final List<TransactionNode.PayType> ignoreTypes = new ArrayList<>();
    private TransactionNode.PayType type = null;
    private SortingMethod sortingMethod = SortingMethod.BY_TIME;
    private boolean sortHighestToLowest = true;
    private boolean traceModeEnabled;
    private int groupUserNamesMax = -1;
    private int groupUserNamesMaxBetween = -1;
    private TimeUnit groupUserNamesMaxBetweenUnit;
    private GroupBy groupBy = GroupBy.NONE;
    private final List<String> toUserNames = new ArrayList<>();
    private final List<String> fromUserNames = new ArrayList<>();
    private double amountFrom = -1;
    private double amountTo = -1;
    private ZonedDateTime timeFrom;
    private ZonedDateTime timeTo;
    private int limit = -1;
    private boolean operatorAnd = true;

    public EngineSettingsGui() {
        this.gui = Gui.gui()
                .title(Component.text("Transaktioner (Indstillinger)"))
                .rows(6)
                .disableAllInteractions()
                .create();

        for (int i : Arrays.asList(7, 8, 16, 17, 25, 26, 34, 43, 44, 45, 46, 47, 48, 49, 52)) {
            this.gui.setItem(i, new GuiItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)));
        }
        for (int i : Arrays.asList(9, 10, 11, 12, 13, 14, 15, 27, 28, 29, 30, 31, 32, 33, 39)) {
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

        this.gui.setItem(35, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 14))
                        .name(Component.text("§cNulstil indstillinger"))
                        .build(), __ -> resetSettings()));

        updateItems();
    }

    public SortingMethod getSortingMethod() {
        return this.sortingMethod;
    }

    public double getAmountTo() {
        return this.amountTo;
    }

    public double getAmountFrom() {
        return this.amountFrom;
    }

    public ZonedDateTime getTimeTo() {
        return this.timeTo;
    }

    public ZonedDateTime getTimeFrom() {
        return this.timeFrom;
    }

    public List<TransactionNode.PayType> getExtraPayTypes() {
        return this.extraTypes;
    }

    public List<TransactionNode.PayType> getIgnorePayTypes() {
        return this.ignoreTypes;
    }

    public TransactionNode.PayType getPayType() {
        return this.type;
    }

    public List<String> getToUserNames() {
        return this.toUserNames;
    }

    public List<String> getFromUserNames() {
        return this.fromUserNames;
    }

    public GroupBy getGroupBy() {
        return this.groupBy;
    }

    public boolean isSortHighestToLowest() {
        return this.sortHighestToLowest;
    }

    public boolean isOperatorAnd() {
        return this.operatorAnd;
    }

    public void open(Player player) {
        this.gui.open(player);
    }

    private void changeSortingMethod(InventoryClickEvent event) {
        if (event.getClick().isRightClick()) {
            this.sortHighestToLowest = !this.sortHighestToLowest;
        } else {
            List<SortingMethod> methods = new ArrayList<>(Arrays.asList(SortingMethod.values()));

            Node.Collection collection = getCollectionFromGroup();
            methods.removeIf(s -> !s.match(collection));

            this.sortingMethod = methods.get((this.sortingMethod.ordinal() + 1) % methods.size());
        }

        updateSortingItem();
    }

    private Node.Collection getCollectionFromGroup() {
        return switch (this.groupBy) {
            case NONE -> Node.Collection.SINGLE;
            case TO_USER, FROM_USER -> Node.Collection.GROUPED;
            case BOTH -> Node.Collection.GROUP_GROUPED;
        };
    }

    private void changePayType(InventoryClickEvent event) {
        multiple: {
            if (this.type == null) {
                this.type = TransactionNode.PayType.PAY;
                if (!event.isShiftClick()) {
                    break multiple;
                }
            }

            if (event.isShiftClick()) {
                if (event.isRightClick()) {
                    this.extraTypes.clear();
                    if (this.ignoreTypes.contains(this.type)) {
                        this.ignoreTypes.remove(this.type);
                    } else {
                        this.ignoreTypes.add(this.type);
                    }
                } else {
                    this.ignoreTypes.clear();
                    if (this.extraTypes.contains(this.type)) {
                        this.extraTypes.remove(this.type);
                    } else {
                        this.extraTypes.add(this.type);
                    }
                }
            } else {
                this.type = TransactionNode.PayType.values()[(this.type.ordinal() + 1) % TransactionNode.PayType.values().length];
            }
        }

        updateTypeItem();
    }

    private void changeOperator() {
        this.operatorAnd = !operatorAnd;
        updateOperatorItem();
    }

    private void configureTraceMode() {
        this.traceModeEnabled = !this.traceModeEnabled;
        updateTraceModeItem();
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

    private void configureLimit(Player player) {
        SignGUI.builder()
                .setLine(0, "Vælg grænse")
                .setLine(1, this.limit != -1 ? String.valueOf(this.limit) : "")
                .setHandler((__, result) -> {
                    String line = result.getLine(1);
                    if (line.isEmpty()) {
                        this.limit = -1;
                        updateLimitItem();
                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                    }

                    int limit;
                    try {
                        limit = Integer.parseInt(line);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cUygldig grænse!");
                        return Collections.singletonList(SignGUIAction.run(() -> configureLimit(player)));
                    }

                    if (limit < 1) {
                        player.sendMessage("§cUgyldig grænse!");
                        return Collections.singletonList(SignGUIAction.run(() -> configureLimit(player)));
                    }

                    this.limit = limit;

                    updateLimitItem();

                    return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                })
                .build()
                .open(player);
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

                    if (Arrays.stream(Arrays.copyOfRange(result.getLines(), 1, 4)).anyMatch(Predicate.not(String::isEmpty))) {
                        if (!added) {
                            player.sendMessage("§cIngen gyldig spiller valgt!");
                            return Collections.singletonList(SignGUIAction.run(() -> addToUser(player)));
                        }

                        updateUsers();
                    }

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

                    if (Arrays.stream(Arrays.copyOfRange(result.getLines(), 1, 4)).anyMatch(Predicate.not(String::isEmpty))) {
                        if (!added) {
                            player.sendMessage("§cIngen gyldig spiller valgt!");
                            return Collections.singletonList(SignGUIAction.run(() -> addFromUser(player)));
                        }

                        updateUsers();
                    }

                    return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                })
                .build()
                .open(player);
    }

    private void configureGrouped(InventoryClickEvent event) {
        if (event.isShiftClick()) {
            if (this.groupBy.equals(GroupBy.NONE)) {
                event.getWhoClicked().sendMessage("§cDu kan ikke konfigurere dette, da du ikke har valgt noget at gruppere efter.");
                return;
            }

            AtomicReference<SignGUI> reference = new AtomicReference<>();
            SignGUI signGui = SignGUI.builder()
                    .setLine(0, "Vælg max tidsinterval")
                    .setLine(1, this.groupUserNamesMaxBetween != -1 ? this.groupUserNamesMaxBetween + " " + UNIT_TO_IDENTIFIER.get(this.groupUserNamesMaxBetweenUnit) : "")
                    .setLine(2, "Vælg max antal")
                    .setLine(3, this.groupUserNamesMax != -1 ? String.valueOf(this.groupUserNamesMax) : "")
                    .setHandler((player, result) -> {
                        String intervalString = result.getLine(1);
                        String maxString = result.getLine(3);
                        if (intervalString.isEmpty() && maxString.isEmpty()) {
                            groupUserNamesMax = -1;
                            groupUserNamesMaxBetween = -1;
                            groupUserNamesMaxBetweenUnit = null;
                            updateGroupItem();
                            return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                        }

                        boolean errorParsingNumber = false;
                        if (!maxString.isEmpty()) {
                            try {
                                groupUserNamesMax = Integer.parseInt(maxString.trim());
                            } catch (NumberFormatException ex) {
                                player.sendMessage("§cUgyldig tal valgt!");
                                errorParsingNumber = true;
                            }
                        } else {
                            groupUserNamesMax = -1;
                        }

                        if (!intervalString.isEmpty()) {
                            String[] parts = Arrays.stream(result.getLine(1).split(" "))
                                    .map(String::trim)
                                    .filter(Predicate.not(String::isEmpty))
                                    .toArray(String[]::new);

                            boolean errorParsingInterval = false;

                            if (parts.length > 0 && parts.length < 3) {
                                String magnitudeString = parts[0].codePoints()
                                        .takeWhile(Character::isDigit)
                                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                                String unitString;
                                if (parts.length == 1) {
                                    unitString = parts[0].replace(magnitudeString, "");
                                } else {
                                    unitString = parts[1];
                                }

                                parse: {
                                    int magnitude;
                                    try {
                                        magnitude = Integer.parseInt(magnitudeString);
                                        Optional<TimeUnit> unitOptional = UNIT_TO_IDENTIFIER.entrySet().stream()
                                                .filter(e -> e.getValue().equalsIgnoreCase(unitString))
                                                .findFirst()
                                                .map(Map.Entry::getKey);
                                        if (unitOptional.isPresent()) {
                                            groupUserNamesMaxBetween = magnitude;
                                            groupUserNamesMaxBetweenUnit = unitOptional.get();
                                            break parse;
                                        }
                                    } catch (NumberFormatException ex) {
                                        // ignore
                                    }
                                    errorParsingInterval = true;
                                }
                            }

                            if (errorParsingInterval) {
                                player.sendMessage("§cUygldig tidsinterval valgt!");
                            }

                            if (errorParsingNumber && errorParsingInterval) {
                                return Collections.singletonList(SignGUIAction.runSync(TransactionEngine.instance, () -> reference.get().open(player)));
                            }
                        } else {
                            groupUserNamesMaxBetween = -1;
                            groupUserNamesMaxBetweenUnit = null;
                        }

                        updateGroupItem();

                        return Collections.singletonList(SIGN_CALLBACK.apply(player, gui));
                    })
                    .build();

            reference.set(signGui);
            signGui.open((Player) event.getWhoClicked());
        } else {
            this.groupBy = GroupBy.values()[(this.groupBy.ordinal() + 1) % GroupBy.values().length];

            if (this.groupBy.equals(GroupBy.NONE)) {
                this.groupUserNamesMaxBetweenUnit = null;
                this.groupUserNamesMaxBetween = -1;
                this.groupUserNamesMax = -1;

                if (this.sortingMethod.match(Node.Collection.GROUPED)) {
                    this.sortingMethod = SortingMethod.BY_TIME;
                }
            }

            updateSortingItem();
            updateGroupItem();
        }
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
                        this.amountFrom = -1;
                        this.amountTo = -1;
                        updateAmountItem();
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
                        } else {
                            this.amountFrom = -1;
                        }

                        if (to != -1) {
                            if (to < from) {
                                throw new NumberFormatException();
                            }
                            this.amountTo = to;
                        } else {
                            this.amountTo = -1;
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
                        this.timeFrom = null;
                        this.timeTo = null;
                        updateTimeItem();
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

                        if (from != null) {
                            ZonedDateTime zonedFrom = from.atZone(ZONE_ID);
                            if (zonedFrom.toInstant().isAfter(Instant.now())) {
                                throw new InvalidDateException();
                            }
                            this.timeFrom = zonedFrom;
                        } else {
                            this.timeFrom = null;
                        }

                        if (to != null) {
                            ZonedDateTime zonedTo = to.atZone(ZONE_ID);
                            if (zonedTo.toInstant().isAfter(Instant.now())) {
                                throw new InvalidDateException();
                            }
                            this.timeTo = zonedTo;
                        } else {
                            this.timeTo = null;
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
        this.extraTypes.clear();
        this.ignoreTypes.clear();
        this.type = null;
        this.sortingMethod = SortingMethod.BY_TIME;
        this.sortHighestToLowest = true;
        this.traceModeEnabled = false;
        this.groupUserNamesMax = -1;
        this.groupBy = GroupBy.NONE;
        this.groupUserNamesMaxBetween = -1;
        this.groupUserNamesMaxBetweenUnit = null;
        this.toUserNames.clear();
        this.fromUserNames.clear();
        this.amountFrom = -1;
        this.amountTo = -1;
        this.timeFrom = null;
        this.timeTo = null;
        this.limit = -1;
        this.operatorAnd = true;

        updateItems();
    }

    private void updateItems() {
        updateUsers();
        updateTraceModeItem();
        updateSortingItem();
        updateAmountItem();
        updateTimeItem();
        updateTypeItem();
        updateGroupItem();
        updateLimitItem();
        updateOperatorItem();

        this.gui.update();
    }

    private void updateExecuteItem() {
        List<Component> executeItemLore = new ArrayList<>();
        executeItemLore.add(Component.text("§8Indstillinger:"));

        List<Component> filters = new ArrayList<>();
        if (!this.fromUserNames.isEmpty()) {
            filters.add(Component.text("§7Fra " + this.fromUserNames.toString().replace("]", "").replace("[", "")));
        }
        if (!this.toUserNames.isEmpty()) {
            filters.add(Component.text("§7Til " + this.toUserNames.toString().replace("]", "").replace("[", "")));
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

        if (!this.ignoreTypes.isEmpty()) {
            filters.add(Component.text("§7Ikke transaktionstype " + this.ignoreTypes.stream()
                    .map(TransactionNode.PayType::toString)
                    .map(String::toLowerCase).toList()
                    .toString()
                    .replace("]", "")
                    .replace("[", "")
            ));
        } else {
            List<TransactionNode.PayType> types = new ArrayList<>(this.extraTypes);
            if (this.type != null && !types.contains(this.type)) {
                types.add(this.type);
            }

            if (!types.isEmpty()) {
                filters.add(Component.text("§7Transaktionstype " + types.stream()
                        .map(TransactionNode.PayType::toString)
                        .map(String::toLowerCase)
                        .toList()
                        .toString()
                        .replace("]", "")
                        .replace("[", "")
                ));
            }
        }

        executeItemLore.add(Component.empty());
        if (filters.isEmpty()) {
            executeItemLore.add(Component.text("§7Ingen filtre"));
        } else {
            executeItemLore.add(Component.text("§8Filtre:"));
            executeItemLore.addAll(filters);
        }

        executeItemLore.add(Component.empty());
        if (!this.groupBy.equals(GroupBy.NONE)) {
            if (this.groupBy == GroupBy.FROM_USER) {
                executeItemLore.add(Component.text("§7Grupperer fra spillere"));
            } else if (this.groupBy == GroupBy.TO_USER) {
                executeItemLore.add(Component.text("§7Grupperer til spillere"));
            }

            if (this.groupUserNamesMax != -1) {
                executeItemLore.add(Component.text("§7Højeste gruppe-antal: " + this.groupUserNamesMax));
            }

            if (this.groupUserNamesMaxBetween != -1) {
                executeItemLore.add(Component.text("§7Højest gruppe-tidsinterval: " + this.groupUserNamesMaxBetween + " " + this.groupUserNamesMaxBetweenUnit.toString().toLowerCase()));
            }
        }

        if (this.traceModeEnabled) {
            executeItemLore.add(Component.text("§7Sporingstilstand slået til"));
        }

        executeItemLore.add(Component.text("§7Sorteres efter " + this.sortingMethod.getName()));
        if (this.sortHighestToLowest) {
            executeItemLore.add(Component.text("§7Sorteres højest til lavest"));
        } else {
            executeItemLore.add(Component.text("§7Sorteres lavest til højest"));
        }

        if (this.limit != -1) {
            executeItemLore.add(Component.text("§7Grænse på " + this.limit + " transaktion" + (this.limit > 1 ? "er" : "")));
        }

        this.gui.updateItem(53, new GuiItem(
                ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 5))
                        .name(Component.text("§aUdfør søgning"))
                        .lore(executeItemLore)
                        .build(), event -> openEngineGui((Player) event.getWhoClicked())));
    }

    private void updateLimitItem() {
        List<Component> limitItemLore = new ArrayList<>();
        if (this.limit == -1) {
            limitItemLore.add(Component.text("§7Ingen grænse §8(Klik)"));
        } else {
            limitItemLore.add(Component.text("§7Grænse på " + this.limit + " transaktioner §8(Klik)"));
        }

        this.gui.updateItem(51, new GuiItem(
                ItemBuilder.from(Material.STICK)
                        .name(Component.text("§eVælg grænse"))
                        .lore(limitItemLore)
                        .build(), event -> configureLimit((Player) event.getWhoClicked())
        ));

        updateExecuteItem();
    }

    private void updateOperatorItem() {
        List<Component> operatorItemLore = new ArrayList<>();
        if (this.operatorAnd) {
            operatorItemLore.add(Component.text("§7Kræver alle filtre §8(Klik)"));
        } else {
            operatorItemLore.add(Component.text("§7Kan have én af alle filtre §8(Klik)"));
        }

        this.gui.updateItem(50, new GuiItem(
                ItemBuilder.from(Material.FLOWER_POT_ITEM)
                        .name(Component.text("§eVælg filtertjek"))
                        .glow(this.operatorAnd)
                        .lore(operatorItemLore)
                        .build(), __ -> changeOperator()
        ));
    }

    private void updateAmountItem() {
        List<Component> amountItemLore = new ArrayList<>();
        if (this.amountFrom == -1 && this.amountTo == -1) {
            amountItemLore.add(Component.text("§7Alle beløber §8(Klik)"));
        } else {
            if (this.amountFrom != 1) {
                amountItemLore.add(Component.text("§7Fra " + this.amountFrom + " emeralder §8(Klik)"));
            }
            if (this.amountTo != -1) {
                amountItemLore.add(Component.text("§7Fra " + this.amountTo + " emeralder §8(Klik)"));
            }
        }

        this.gui.updateItem(36, new GuiItem(
                ItemBuilder.from(Material.GOLD_INGOT)
                        .name(Component.text("§6Indstil beløber"))
                        .lore(amountItemLore)
                        .build(), event -> configureAmountRange((Player) event.getWhoClicked())));

        updateExecuteItem();
    }

    private void updateTimeItem() {
        List<Component> timeItemLore = new ArrayList<>();
        if (this.timeFrom == null && this.timeTo == null) {
            timeItemLore.add(Component.text("§7Alle tidspunkter §8(Klik)"));
        } else {
            if (this.timeFrom != null) {
                timeItemLore.add(Component.text("§7Fra " + TIME_FORMATTER.format(this.timeFrom) + " §8(Klik)"));
            }
            if (this.timeTo != null) {
                timeItemLore.add(Component.text("§7Fra " + TIME_FORMATTER.format(this.timeTo) + " §8(Klik)"));
            }
        }

        this.gui.updateItem(37, new GuiItem(
                ItemBuilder.from(Material.COMPASS)
                        .name(Component.text("§6Indstil tidspunkter"))
                        .lore(timeItemLore)
                        .build(), event -> configureTimeRange((Player) event.getWhoClicked())));

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

    private void updateTypeItem() {
        List<Component> typeItemLore = new ArrayList<>();
        typeItemLore.add(Component.text("§7Shift+§cHøjreklik §7for at ignorere"));
        typeItemLore.add(Component.text("§7Shift+§aVenstreklik §7for at tilføje"));
        typeItemLore.add(Component.text("§7Husk man kan have mere end en"));
        typeItemLore.add(Component.empty());

        typeItemLore.add(Component.text("§7Valgt type: §8(Klik) " + (this.type == null ? "§a(Alle)" : "")));
        for (TransactionNode.PayType type : TransactionNode.PayType.values()) {
            String colour = type.equals(this.type) ? "§e" : "§8";

            String extra = "";
            if (this.extraTypes.contains(type)) {
                extra = "§a+ ";
            }
            if (this.ignoreTypes.contains(type)) {
                extra = "§c! ";
            }
            typeItemLore.add(Component.text(extra + colour + " - " + type.name().toLowerCase()));
        }
        typeItemLore.add(Component.empty());

        this.gui.updateItem(38, new GuiItem(
                ItemBuilder.from(Material.GLASS_BOTTLE)
                        .name(Component.text("§6Vælg transaktionstype"))
                        .lore(typeItemLore)
                        .build(), this::changePayType));

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
        Node.Collection collection = getCollectionFromGroup();
        for (SortingMethod method : SortingMethod.values()) {
            if (method.match(collection)) {
                String colour = this.sortingMethod == method ? "§e" : "§8";
                sortingItemLore.add(Component.text(colour + " - " + method.getName()));
            }
        }
        sortingItemLore.add(Component.empty());

        this.gui.updateItem(42, new GuiItem(ItemBuilder.from(Material.BOOK_AND_QUILL)
                .name(Component.text("§6Sortér efter"))
                .lore(sortingItemLore)
                .glow(this.sortHighestToLowest)
                .build(), this::changeSortingMethod));

        updateExecuteItem();
    }

    private void updateGroupItem() {
        List<Component> groupItemLore = new ArrayList<>();
        if (this.groupUserNamesMax == -1) {
            groupItemLore.add(Component.text("§7Ingen kapacitet på gruppe-antal §8(Shift+Klik)"));
        } else {
            groupItemLore.add(Component.text("§7Højeste gruppe-antal: " + this.groupUserNamesMax + " §8(Shift+Klik)"));
        }
        if (this.groupUserNamesMaxBetween == -1) {
            groupItemLore.add(Component.text("§7Ingen grænse på gruppe-tidsinterval §8(Shift+Klik)"));
        } else {
            groupItemLore.add(Component.text("§7Højeste gruppe-tidsinterval: " + this.groupUserNamesMaxBetween + UNIT_TO_IDENTIFIER.get(this.groupUserNamesMaxBetweenUnit) + " §8(Shift+Klik)"));
        }
        groupItemLore.add(Component.empty());

        groupItemLore.add(Component.text("§7Valgt gruppering: §8(Klik)"));
        for (GroupBy groupBy : GroupBy.values()){
            String colour = this.groupBy == groupBy ? "§e" : "§8";
            groupItemLore.add(Component.text(colour + " - " + groupBy.getName()));
        }

        this.gui.updateItem(40, new GuiItem(
                ItemBuilder.from(Material.BUCKET)
                        .name(Component.text("§6Gruppér efter"))
                        .lore(groupItemLore)
                        .glow(!this.groupBy.equals(GroupBy.NONE))
                        .build(), this::configureGrouped));

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

                    Bukkit.getScheduler().runTask(TransactionEngine.instance, () -> {
                        if (player.getOpenInventory().getTopInventory() == null) {
                            // the request returned an empty query, open the settings gui again
                            open(player);
                        }
                    });
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
            TransactionRequestBuilder<EngineRequest.RequestBuilder<SingleTransactionNode>, EngineRequest<SingleTransactionNode>> builder =
                    TransactionRequestBuilder.builder(CACHE, TransactionEngine.instance.getSettings(), controller, controller.getRequester());

            builder.to(toUserNames.toArray(String[]::new));
            builder.from(fromUserNames.toArray(String[]::new));

            if (this.amountFrom != -1) {
                builder.from(this.amountFrom);
            }
            if (this.amountTo != -1) {
                builder.to(this.amountTo);
            }
            if (this.timeFrom != null) {
                builder.from(this.timeFrom);
            }
            if (this.timeTo != null) {
                builder.to(this.timeTo);
            }

            if (!this.ignoreTypes.isEmpty()) {
                builder.isNot(this.ignoreTypes.toArray(TransactionNode.PayType[]::new));
            } else {
                List<TransactionNode.PayType> types = new ArrayList<>(this.extraTypes);
                if (this.type != null && !types.contains(this.type)) {
                    types.add(this.type);
                }

                if (!types.isEmpty()) {
                    builder.is(types.toArray(TransactionNode.PayType[]::new));
                }
            }

            if (this.operatorAnd) {
                builder.setOperator(QueryFilter.Operator.and());
            } else {
                builder.setOperator(QueryFilter.Operator.or());
            }

            EngineQuery<SingleTransactionNode> query = Engine.queryFromCache(builder.build());
            if (this.traceModeEnabled) {
                query = Engine.trace(query);
            }

            EngineQuery<? extends TransactionNode> finalQuery;
            if (!this.groupBy.equals(GroupBy.NONE)) {
                List<PostQueryTransformer.GroupBy.GroupOperator<SingleTransactionNode.Target, SingleTransactionNode>> operators = new ArrayList<>();
                if (this.groupUserNamesMax != -1) {
                    operators.add(PostQueryTransformer.GroupBy.GroupOperator.max(this.groupUserNamesMax));
                }
                if (this.groupUserNamesMaxBetween != -1) {
                    operators.add(PostQueryTransformer.GroupBy.GroupOperator.maxBetween(SingleTransactionNode::time, this.groupUserNamesMaxBetween, this.groupUserNamesMaxBetweenUnit));
                }
                PostQueryTransformer.GroupBy.GroupOperator<SingleTransactionNode.Target, SingleTransactionNode> operator = PostQueryTransformer.GroupBy.GroupOperator.mix(operators);

                if (!this.groupBy.equals(GroupBy.BOTH)) {
                    Function<SingleTransactionNode, String> func;
                    TransactionNode.GroupedTransactionNode.Bound bound;
                    if (this.groupBy == GroupBy.FROM_USER) {
                        bound = TransactionNode.GroupedTransactionNode.Bound.FROM;
                        func = SingleTransactionNode::fromUserName;
                    } else if (this.groupBy == GroupBy.TO_USER) {
                        bound = TransactionNode.GroupedTransactionNode.Bound.TO;
                        func = SingleTransactionNode::toUserName;
                    } else {
                        throw new IllegalStateException();
                    }
                    finalQuery = group(query, operator, bound, func);
                } else {
                    finalQuery = query.transform(
                        PostQueryTransformer.GroupBy.groupBy(operator,
                        new PostQueryTransformer.GroupBy.GroupCollector<SingleTransactionNode, SingleTransactionNode.Target, TransactionNode.GroupedBothWayTransactionNode, String>() {

                            @Override
                            public TransactionNode.GroupedBothWayTransactionNode collect(Collection<SingleTransactionNode.Target> nodes) {
                                Collection<SingleTransactionNode.Target> from = nodes.stream()
                                        .filter(t -> t.bound().equals(TransactionNode.GroupedTransactionNode.Bound.FROM))
                                        .collect(Collectors.toList());
                                Collection<SingleTransactionNode.Target> to = nodes.stream()
                                        .filter(t -> t.bound().equals(TransactionNode.GroupedTransactionNode.Bound.TO))
                                        .collect(Collectors.toList());

                                String user = from.stream()
                                        .map(Node.Linked::node)
                                        .map(SingleTransactionNode::fromUserName)
                                        .findFirst()
                                        .orElse(
                                            to.stream()
                                                    .map(Node.Linked::node)
                                                    .map(SingleTransactionNode::toUserName)
                                                    .findFirst()
                                                    .orElse(null)
                                        );
                                if (user == null) {
                                    // ?
                                    throw new IllegalStateException();
                                }

                                return new TransactionNode.GroupedBothWayTransactionNode(user, from, to);
                            }

                            @Override
                            public void applyToGroup(SingleTransactionNode node, Map<String, List<SingleTransactionNode.Target>> groups) {
                                insert(node.toUserName(), new SingleTransactionNode.Target(TransactionNode.GroupedTransactionNode.Bound.TO, node), groups);
                                insert(node.fromUserName(), new SingleTransactionNode.Target(TransactionNode.GroupedTransactionNode.Bound.FROM, node), groups);
                            }
                        })
                    );
                }
            } else {
                finalQuery = query;
            }

            finalQuery = Engine.sort(getCollectionFromGroup(), this.sortingMethod, finalQuery);
            if (this.sortHighestToLowest) {
                finalQuery = finalQuery.transform(PostQueryTransformer.reversed());
            }

            if (this.limit != -1) {
                finalQuery.limit(this.limit);
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

    @SuppressWarnings("unchecked")
    private EngineQuery<TransactionNode.GroupedTransactionNode> group(EngineQuery<SingleTransactionNode> query, PostQueryTransformer.GroupBy.GroupOperator<SingleTransactionNode.Target, SingleTransactionNode> operator, TransactionNode.GroupedTransactionNode.Bound bound, Function<SingleTransactionNode, String> keyGenerator) {
        return query.transform(PostQueryTransformer.GroupBy.groupBy(
                (group, node) -> operator.checkGroup((List<SingleTransactionNode.Target>) (List<?>) group, node),
                new PostQueryTransformer.GroupBy.GroupCollector.Direct<SingleTransactionNode, TransactionNode.GroupedTransactionNode, String>() {

                    @Override
                    public TransactionNode.GroupedTransactionNode collect(Collection<SingleTransactionNode> nodes) {
                        return new TransactionNode.GroupedTransactionNode(nodes, bound);
                    }

                    @Override
                    public String getKey(SingleTransactionNode node) {
                        return keyGenerator.apply(node);
                    }
                })
        );
    }

    public enum GroupBy {
        NONE("ingen"), TO_USER("til spiller"), FROM_USER("fra spiller"), BOTH("begge");

        private final String name;

        GroupBy(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
