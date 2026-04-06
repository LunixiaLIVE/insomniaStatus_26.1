package net.lunix.insomniastatus.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen {

    private static final int COLS      = 9;
    private static final int CELL_SIZE = 18;
    private static final int TAB_H     = 26;

    private final Screen         parent;
    private final Consumer<Item> callback;

    private List<TabEntry>  tabs         = new ArrayList<>();
    private int             selectedTab  = 0;
    private int             scrollOffset = 0;

    private EditBox         searchBox;
    private List<ItemStack> displayedItems = new ArrayList<>();

    // Grid layout
    private int gridX;
    private int gridY;
    private int gridW;
    private int visibleRows;

    private record TabEntry(Component name, ItemStack icon, List<ItemStack> items) {}

    public ItemPickerScreen(Screen parent, Consumer<Item> callback) {
        super(Component.literal("Select Icon"));
        this.parent   = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        tabs = buildTabs();

        gridW       = COLS * CELL_SIZE;
        gridX       = (this.width - gridW) / 2;
        int searchY = TAB_H + 2;
        gridY       = searchY + 20 + 4;
        int gridH   = this.height - gridY - 30;
        visibleRows = Math.max(1, gridH / CELL_SIZE);

        searchBox = new EditBox(this.font, gridX, searchY, gridW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search items..."));
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

    private List<TabEntry> buildTabs() {
        List<TabEntry> result = new ArrayList<>();

        // "All" tab shows every registered item
        result.add(new TabEntry(Component.literal("All"), new ItemStack(Items.CHEST), Collections.emptyList()));

        try {
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                CreativeModeTab.Type type = tab.getType();
                if (type == CreativeModeTab.Type.SEARCH  ||
                    type == CreativeModeTab.Type.HOTBAR  ||
                    type == CreativeModeTab.Type.INVENTORY) continue;
                Collection<ItemStack> items = tab.getDisplayItems();
                if (items.isEmpty()) continue;
                result.add(new TabEntry(tab.getDisplayName(), tab.getIconItem(), new ArrayList<>(items)));
            }
        } catch (Exception ignored) {
            // Fall back: only the "All" tab
        }

        return result;
    }

    private void updateDisplayedItems() {
        String search = (searchBox != null) ? searchBox.getValue().trim().toLowerCase() : "";

        if (!search.isEmpty()) {
            displayedItems = BuiltInRegistries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .filter(i -> {
                    Identifier id = BuiltInRegistries.ITEM.getKey(i);
                    return id != null && (id.getPath().contains(search)
                        || i.getDescriptionId().toLowerCase().contains(search));
                })
                .map(ItemStack::new)
                .toList();
        } else if (selectedTab == 0) {
            displayedItems = BuiltInRegistries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .map(ItemStack::new)
                .toList();
        } else {
            displayedItems = new ArrayList<>(tabs.get(selectedTab).items());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xC0101010);

        g.centeredText(font, title, this.width / 2, 4, 0xFFFFFF);

        // Tab bar
        int tabCount = tabs.size();
        int tabW     = Math.max(20, Math.min(28, gridW / tabCount - 1));

        for (int i = 0; i < tabCount; i++) {
            TabEntry tab = tabs.get(i);
            int tx = gridX + i * (tabW + 1);
            boolean active = (i == selectedTab) && (searchBox == null || searchBox.getValue().isEmpty());
            g.fill(tx, 14, tx + tabW, 14 + TAB_H, active ? 0xFFC6C6C6 : 0xFF8B8B8B);
            g.item(tab.icon(), tx + (tabW - 16) / 2, 14 + (TAB_H - 16) / 2);
            if (active) g.fill(tx, 14 + TAB_H - 1, tx + tabW, 14 + TAB_H, 0xFFFFFFFF);
        }

        // Item grid
        int gh = visibleRows * CELL_SIZE;
        g.fill(gridX - 1, gridY - 1, gridX + gridW + 1, gridY + gh + 1, 0xFF555555);
        g.fill(gridX, gridY, gridX + gridW, gridY + gh, 0xFF1E1E1E);

        int startIdx = scrollOffset * COLS;
        int count    = Math.min(visibleRows * COLS, displayedItems.size() - startIdx);
        int hoveredIdx = -1;
        for (int i = 0; i < count; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int ix  = gridX + col * CELL_SIZE + 1;
            int iy  = gridY + row * CELL_SIZE + 1;
            boolean hovered = mouseX >= ix && mouseX < ix + CELL_SIZE - 2
                           && mouseY >= iy && mouseY < iy + CELL_SIZE - 2;
            if (hovered) {
                g.fill(ix - 1, iy - 1, ix + CELL_SIZE - 1, iy + CELL_SIZE - 1, 0x60FFFFFF);
                hoveredIdx = startIdx + i;
            }
            g.item(displayedItems.get(startIdx + i), ix, iy);
        }

        // Hovered item name tooltip
        if (hoveredIdx >= 0) {
            String name = displayedItems.get(hoveredIdx).getHoverName().getString();
            int tx = Math.min(mouseX + 6, this.width - font.width(name) - 4);
            int ty = mouseY - 12;
            g.fill(tx - 2, ty - 2, tx + font.width(name) + 2, ty + 10, 0xC0000000);
            g.text(font, name, tx, ty, 0xFFFFFF);
        }

        // Scroll bar
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
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            // Tab bar click
            int tabCount = tabs.size();
            int tabW     = Math.max(20, Math.min(28, gridW / tabCount - 1));
            for (int i = 0; i < tabCount; i++) {
                int tx = gridX + i * (tabW + 1);
                if (mouseX >= tx && mouseX < tx + tabW && mouseY >= 14 && mouseY < 14 + TAB_H) {
                    selectedTab  = i;
                    scrollOffset = 0;
                    if (searchBox != null) searchBox.setValue("");
                    updateDisplayedItems();
                    return true;
                }
            }

            // Item grid click
            int startIdx = scrollOffset * COLS;
            int count    = Math.min(visibleRows * COLS, displayedItems.size() - startIdx);
            for (int i = 0; i < count; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int ix  = gridX + col * CELL_SIZE + 1;
                int iy  = gridY + row * CELL_SIZE + 1;
                if (mouseX >= ix && mouseX < ix + CELL_SIZE - 2
                 && mouseY >= iy && mouseY < iy + CELL_SIZE - 2) {
                    callback.accept(displayedItems.get(startIdx + i).getItem());
                    minecraft.setScreen(parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (displayedItems.size() + COLS - 1) / COLS;
        if (totalRows > visibleRows) {
            scrollOffset = Math.max(0, Math.min(totalRows - visibleRows,
                scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
