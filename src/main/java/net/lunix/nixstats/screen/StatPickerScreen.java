package net.lunix.nixstats.screen;

import net.lunix.nixstats.StatEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StatPickerScreen extends Screen {

    private static final int COLS       = 9;
    private static final int CELL_SIZE  = 18;
    private static final int CAT_BAR_H  = 20;
    private static final int TEXT_ROW_H = 13;

    // Internal type keys — General sub-types use "general_*" here but store "custom" in StatEntry
    private static final String[] STAT_TYPES = {
        "phantom",
        "general_counts", "general_distances", "general_time",
        "block_mined", "item_used", "item_crafted",
        "item_broken", "item_picked_up", "item_dropped",
        "entity_killed", "entity_killed_by"
    };
    private static final String[] CAT_LABELS = {
        "Phantom Timer",
        "General: Counts", "General: Distances", "General: Time",
        "Items: Mined", "Items: Used", "Items: Crafted",
        "Items: Broken", "Items: Picked Up", "Items: Dropped",
        "Mobs: Killed", "Mobs: Killed By"
    };

    // Stat type helpers — used by both the picker and the sidebar formatter
    public static boolean isTimeStat(String id) {
        String path = stripNamespace(id);
        return path.endsWith("_time") || path.startsWith("time_");
    }
    public static boolean isDistanceStat(String id) {
        return stripNamespace(id).endsWith("_one_cm");
    }
    public static boolean isCountStat(String id) {
        return !isTimeStat(id) && !isDistanceStat(id);
    }
    private static String stripNamespace(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    // Lazily built from the registry so all stats appear regardless of MC version
    private static String[][] cachedCounts    = null;
    private static String[][] cachedDistances = null;
    private static String[][] cachedTime      = null;

    private static String[][] buildCustomStats(String category) {
        List<String[]> result = new ArrayList<>();
        BuiltInRegistries.CUSTOM_STAT.stream().forEach(rl -> {
            String id = rl.toString();
            boolean include = switch (category) {
                case "distances" -> isDistanceStat(id);
                case "time"      -> isTimeStat(id);
                case "counts"    -> isCountStat(id);
                default          -> false;
            };
            if (include) {
                String path  = rl.getPath();
                String label = isDistanceStat(id) ? toLabel(path.substring(0, path.length() - 7)) : toLabel(path);
                result.add(new String[]{id, label});
            }
        });
        result.sort(Comparator.comparing(a -> a[1]));
        return result.toArray(new String[0][]);
    }

    private final Screen              parent;
    private final Consumer<StatEntry> callback;

    private int categoryIdx = 0;

    private EditBox         searchBox;
    private List<ItemStack> displayedItems       = new ArrayList<>();
    private List<String[]>  displayedCustomStats = new ArrayList<>();

    private int gridX, gridY, gridW, gridH;
    private int visibleRows;
    private int scrollOffset = 0;

    private int catBarY, searchY;

    private Set<Item> craftableItemsCache = null;

    public StatPickerScreen(Screen parent, Consumer<StatEntry> callback) {
        super(Component.literal("Add Stat"));
        this.parent   = parent;
        this.callback = callback;
    }

    private boolean isTextList() {
        String t = STAT_TYPES[categoryIdx];
        return t.equals("general_counts") || t.equals("general_distances") || t.equals("general_time");
    }

    private String[][] currentTextData() {
        return switch (STAT_TYPES[categoryIdx]) {
            case "general_counts" -> {
                if (cachedCounts == null) cachedCounts = buildCustomStats("counts");
                yield cachedCounts;
            }
            case "general_distances" -> {
                if (cachedDistances == null) cachedDistances = buildCustomStats("distances");
                yield cachedDistances;
            }
            case "general_time" -> {
                if (cachedTime == null) cachedTime = buildCustomStats("time");
                yield cachedTime;
            }
            default -> new String[0][];
        };
    }

    @Override
    protected void init() {
        gridW = COLS * CELL_SIZE;
        gridX = (this.width - gridW) / 2;

        catBarY = 14;
        searchY = catBarY + CAT_BAR_H + 4;
        gridY   = searchY + 18 + 4;

        gridH       = this.height - gridY - 30;
        visibleRows = Math.max(1, gridH / CELL_SIZE);

        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            categoryIdx = (categoryIdx + STAT_TYPES.length - 1) % STAT_TYPES.length;
            scrollOffset = 0;
            if (searchBox != null) searchBox.setValue("");
            rebuildWidgets();
        }).bounds(gridX, catBarY, 20, CAT_BAR_H).build());

        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            categoryIdx = (categoryIdx + 1) % STAT_TYPES.length;
            scrollOffset = 0;
            if (searchBox != null) searchBox.setValue("");
            rebuildWidgets();
        }).bounds(gridX + gridW - 20, catBarY, 20, CAT_BAR_H).build());

        if (categoryIdx == 0) {
            addRenderableWidget(Button.builder(Component.literal("Add Phantom Timer"), btn -> {
                callback.accept(StatEntry.phantom());
                minecraft.setScreen(parent);
            }).bounds(this.width / 2 - 70, gridY + 10, 140, 20).build());
        }

        searchBox = new EditBox(this.font, gridX, searchY, gridW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            updateDisplayedItems();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn ->
            minecraft.setScreen(parent)
        ).bounds(this.width / 2 - 30, this.height - 24, 60, 20).build());

        updateDisplayedItems();
    }

    private void updateDisplayedItems() {
        String search = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";

        if (isTextList()) {
            displayedItems = List.of();
            displayedCustomStats = new ArrayList<>();
            for (String[] stat : currentTextData()) {
                if (search.isEmpty() || stat[1].toLowerCase().contains(search) || stat[0].contains(search))
                    displayedCustomStats.add(stat);
            }
            return;
        }

        if (categoryIdx == 0) {
            displayedItems = List.of();
            displayedCustomStats = List.of();
            return;
        }

        String type    = STAT_TYPES[categoryIdx];
        boolean isEntity  = type.startsWith("entity_");
        boolean isBlock   = type.equals("block_mined");
        boolean isBroken  = type.equals("item_broken");
        boolean isCrafted = type.equals("item_crafted");

        if (isCrafted && craftableItemsCache == null) {
            var mc  = net.minecraft.client.Minecraft.getInstance();
            var srv = mc.getSingleplayerServer();
            if (srv != null) {
                craftableItemsCache = new HashSet<>();
                for (RecipeHolder<?> h : srv.getRecipeManager().getRecipes()) {
                    if (!(h.value() instanceof CraftingRecipe cr)) continue;
                    for (var disp : cr.display()) {
                        SlotDisplay sd = disp.result();
                        if (sd instanceof SlotDisplay.ItemStackSlotDisplay isd) {
                            Item it = isd.stack().item().value();
                            if (it != Items.AIR) craftableItemsCache.add(it);
                        }
                    }
                }
            }
        }

        displayedItems = BuiltInRegistries.ITEM.stream()
            .filter(i -> i != Items.AIR)
            .filter(i -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(i);
                if (id == null) return false;
                if (isEntity)  return id.getPath().endsWith("_spawn_egg");
                if (isBlock)   return i instanceof BlockItem bi
                    && bi.getBlock().defaultDestroyTime() >= 0
                    && bi.getBlock().defaultDestroyTime() < 3_600_000;
                if (isBroken)  return new ItemStack(i).isDamageableItem();
                if (isCrafted) {
                    if (craftableItemsCache != null) return craftableItemsCache.contains(i);
                    return !(i instanceof BlockItem) && !id.getPath().endsWith("_spawn_egg");
                }
                return !id.getPath().endsWith("_spawn_egg");
            })
            .filter(i -> {
                if (search.isEmpty()) return true;
                Identifier id = BuiltInRegistries.ITEM.getKey(i);
                return id != null && (id.getPath().contains(search)
                    || i.getDescriptionId().toLowerCase().contains(search));
            })
            .map(ItemStack::new)
            .toList();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xC0101010);
        g.centeredText(font, title, this.width / 2, 4, 0xFFFFFF);

        String catLabel = CAT_LABELS[categoryIdx];
        g.fill(gridX, catBarY, gridX + gridW, catBarY + CAT_BAR_H, 0xFF2A2A2A);
        g.fill(gridX, catBarY, gridX + gridW, catBarY + 1, 0xFF636363);
        g.fill(gridX, catBarY + CAT_BAR_H - 1, gridX + gridW, catBarY + CAT_BAR_H, 0xFF636363);
        g.centeredText(font, Component.literal(catLabel), this.width / 2, catBarY + 6, 0xFFFFAA00);

        if (categoryIdx == 0) {
            g.centeredText(font, Component.literal("No item selection needed."),
                this.width / 2, gridY + 10, 0xAAAAAA);
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            return;
        }

        if (isTextList()) {
            renderTextList(g, mouseX, mouseY);
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            return;
        }

        int gh = visibleRows * CELL_SIZE;
        g.fill(gridX - 1, gridY - 1, gridX + gridW + 1, gridY + gh + 1, 0xFF555555);
        g.fill(gridX, gridY, gridX + gridW, gridY + gh, 0xFF1E1E1E);

        int startIdx = scrollOffset * COLS;
        int count    = Math.min(visibleRows * COLS, displayedItems.size() - startIdx);
        ItemStack hoveredStack = null;
        for (int i = 0; i < count; i++) {
            int col = i % COLS, row = i / COLS;
            int ix = gridX + col * CELL_SIZE + 1;
            int iy = gridY + row * CELL_SIZE + 1;
            boolean hovered = mouseX >= ix && mouseX < ix + CELL_SIZE - 2
                           && mouseY >= iy && mouseY < iy + CELL_SIZE - 2;
            if (hovered) {
                g.fill(ix - 1, iy - 1, ix + CELL_SIZE - 1, iy + CELL_SIZE - 1, 0x60FFFFFF);
                hoveredStack = displayedItems.get(startIdx + i);
            }
            g.item(displayedItems.get(startIdx + i), ix, iy);
        }

        int totalRows = (displayedItems.size() + COLS - 1) / COLS;
        if (totalRows > visibleRows) {
            int sbX    = gridX + gridW + 2;
            int sbH    = gh;
            int thumbH = Math.max(10, sbH * visibleRows / totalRows);
            int maxOff = totalRows - visibleRows;
            int thumbY = gridY + (maxOff > 0 ? (sbH - thumbH) * scrollOffset / maxOff : 0);
            g.fill(sbX, gridY, sbX + 4, gridY + sbH, 0xFF444444);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFFAAAAAA);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        if (hoveredStack != null) {
            g.tooltip(font,
                List.of(ClientTooltipComponent.create(hoveredStack.getHoverName().getVisualOrderText())),
                mouseX, mouseY,
                DefaultTooltipPositioner.INSTANCE,
                (Identifier) null);
        }
    }

    private void renderTextList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int tvr = Math.max(1, gridH / TEXT_ROW_H);
        int gh  = tvr * TEXT_ROW_H;

        g.fill(gridX - 1, gridY - 1, gridX + gridW + 1, gridY + gh + 1, 0xFF555555);
        g.fill(gridX, gridY, gridX + gridW, gridY + gh, 0xFF1E1E1E);

        int count = Math.min(tvr, displayedCustomStats.size() - scrollOffset);
        for (int i = 0; i < count; i++) {
            String[] stat = displayedCustomStats.get(scrollOffset + i);
            int ry = gridY + i * TEXT_ROW_H;
            boolean hovered = mouseX >= gridX && mouseX < gridX + gridW
                           && mouseY >= ry && mouseY < ry + TEXT_ROW_H;
            if (hovered) g.fill(gridX, ry, gridX + gridW, ry + TEXT_ROW_H, 0x60FFFFFF);
            g.text(font, Component.literal(stat[1]), gridX + 3, ry + 2, 0xFFFFFFFF);
        }

        int total = displayedCustomStats.size();
        if (total > tvr) {
            int sbX    = gridX + gridW + 2;
            int thumbH = Math.max(10, gh * tvr / total);
            int maxOff = total - tvr;
            int thumbY = gridY + (maxOff > 0 ? (gh - thumbH) * scrollOffset / maxOff : 0);
            g.fill(sbX, gridY, sbX + 4, gridY + gh, 0xFF444444);
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0) {
            double mx = event.x(), my = event.y();

            if (isTextList()) {
                int tvr   = Math.max(1, gridH / TEXT_ROW_H);
                int count = Math.min(tvr, displayedCustomStats.size() - scrollOffset);
                for (int i = 0; i < count; i++) {
                    int ry = gridY + i * TEXT_ROW_H;
                    if (mx >= gridX && mx < gridX + gridW && my >= ry && my < ry + TEXT_ROW_H) {
                        String[] stat = displayedCustomStats.get(scrollOffset + i);
                        callback.accept(new StatEntry("custom", stat[0], stat[1]));
                        minecraft.setScreen(parent);
                        return true;
                    }
                }
                return super.mouseClicked(event, consumed);
            }

            if (categoryIdx != 0) {
                int startIdx = scrollOffset * COLS;
                int count    = Math.min(visibleRows * COLS, displayedItems.size() - startIdx);
                for (int i = 0; i < count; i++) {
                    int col = i % COLS, row = i / COLS;
                    int ix = gridX + col * CELL_SIZE + 1;
                    int iy = gridY + row * CELL_SIZE + 1;
                    if (mx >= ix && mx < ix + CELL_SIZE - 2 && my >= iy && my < iy + CELL_SIZE - 2) {
                        callback.accept(buildEntry(displayedItems.get(startIdx + i).getItem()));
                        minecraft.setScreen(parent);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (isTextList()) {
            int tvr   = Math.max(1, gridH / TEXT_ROW_H);
            int total = displayedCustomStats.size();
            if (total > tvr) {
                scrollOffset = Math.max(0, Math.min(total - tvr,
                    scrollOffset - (int) Math.signum(scrollY)));
                return true;
            }
            return super.mouseScrolled(mx, my, scrollX, scrollY);
        }
        int totalRows = (displayedItems.size() + COLS - 1) / COLS;
        if (totalRows > visibleRows) {
            scrollOffset = Math.max(0, Math.min(totalRows - visibleRows,
                scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private StatEntry buildEntry(Item item) {
        String type = STAT_TYPES[categoryIdx];
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return StatEntry.phantom();

        boolean isEntity = type.startsWith("entity_");
        boolean isBlock  = type.equals("block_mined");
        String targetId, baseName;
        if (isEntity) {
            String path       = id.getPath();
            String entityPath = path.endsWith("_spawn_egg") ? path.substring(0, path.length() - 10) : path;
            targetId = id.getNamespace() + ":" + entityPath;
            baseName = toLabel(entityPath);
        } else if (isBlock && item instanceof BlockItem bi) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
            targetId = blockId != null ? blockId.toString() : id.toString();
            baseName = toLabel(id.getPath());
        } else {
            targetId = id.toString();
            baseName = toLabel(id.getPath());
        }
        String label = switch (type) {
            case "block_mined"      -> baseName + " Mined";
            case "item_used"        -> baseName + " Used";
            case "item_crafted"     -> baseName + " Crafted";
            case "item_broken"      -> baseName + " Broken";
            case "item_picked_up"   -> baseName + " Picked Up";
            case "item_dropped"     -> baseName + " Dropped";
            case "entity_killed"    -> baseName + " Killed";
            case "entity_killed_by" -> "Killed by " + baseName;
            default -> baseName;
        };
        return new StatEntry(type, targetId, label);
    }

    private static String toLabel(String path) {
        String[] parts = path.replace('/', ' ').replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
