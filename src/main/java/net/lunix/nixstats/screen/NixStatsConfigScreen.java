package net.lunix.nixstats.screen;

import net.lunix.nixstats.NixStatsConfig;
import net.lunix.nixstats.StatEntry;
import net.lunix.nixstats.StatSidebar;
import net.lunix.nixstats.NixStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NixStatsConfigScreen extends Screen {

    private static final int[] PRESET_COLORS = {
        0xFF55FF55, 0xFF00AA00, 0xFF55FFFF, 0xFF00AAAA,
        0xFF5555FF, 0xFF0000AA, 0xFFFF55FF, 0xFFAA00AA,
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFFFFFFFF
    };

    private static boolean tempLoaded        = false;
    private static String  tempTitle;
    private static float   tempScale;
    private static float   tempTextScale;
    private static String  tempFont;
    private static int     tempColPad;
    private static int     tempSyncInterval;
    private static int     tempColorRested;
    private static int     tempColorWarning;
    private static int     tempColorCritical;
    private static float   tempThresholdWarning;
    private static float   tempThresholdCritical;
    private static List<StatEntry> tempStats;
    private static int     swatchSelRested;
    private static int     swatchSelWarning;
    private static int     swatchSelCritical;

    private static final String[] COLOR_ROW_LABELS = { "Rested:", "Warning:", "Critical:" };

    private final Screen parent;

    // Layout — set each init
    private int panelX, panelW;
    private int boxTop, boxBottom;
    private int titleEditY, scaleSliderY, textScaleSliderY, colPadSliderY, fontToggleY, syncSliderY;
    private int colorHeaderY, colorRowsBaseY, warnSliderY, critSliderY;
    private int statsHeaderY, statsListBaseY, addBtnY, buttonsY;
    private int innerX, innerW;

    public NixStatsConfigScreen(Screen parent) {
        super(Component.literal("nixStats Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!tempLoaded) {
            NixStatsConfig cfg = NixStatsConfig.get();
            tempTitle             = cfg.sidebarTitle != null ? cfg.sidebarTitle : "nixStats";
            tempScale             = cfg.scale;
            tempTextScale         = cfg.textScale > 0 ? cfg.textScale : 1.0f;
            tempFont              = cfg.font != null ? cfg.font : "default";
            tempColPad            = Math.max(0, Math.min(20, cfg.colPad));
            tempSyncInterval      = Math.max(1, Math.min(60, cfg.syncInterval));
            tempColorRested       = cfg.colorRested;
            tempColorWarning      = cfg.colorWarning;
            tempColorCritical     = cfg.colorCritical;
            tempThresholdWarning  = cfg.thresholdWarning;
            tempThresholdCritical = cfg.thresholdCritical;
            tempStats             = deepCopy(cfg.stats);
            swatchSelRested       = findSwatch(tempColorRested);
            swatchSelWarning      = findSwatch(tempColorWarning);
            swatchSelCritical     = findSwatch(tempColorCritical);
            tempLoaded = true;
        }

        panelW = Math.min(250, this.width - 16);
        panelX = Math.max(4, (this.width - panelW) / 2);
        innerX = panelX + 6;
        innerW = panelW - 12;

        int n = tempStats != null ? tempStats.size() : 0;
        int innerH = 298 + n * 16;
        int boxH   = innerH + 10;
        int panelY = Math.max(8, (this.height - boxH - 30) / 2);

        boxTop = panelY;
        int y = panelY + 5;

        titleEditY       = y; y += 22;
        scaleSliderY     = y; y += 19;
        textScaleSliderY = y; y += 19;
        colPadSliderY    = y; y += 19;
        fontToggleY      = y; y += 22;
        syncSliderY      = y; y += 24;
        colorHeaderY  = y; y += 14;
        colorRowsBaseY = y; y += 78;
        warnSliderY   = y; y += 19;
        critSliderY   = y; y += 24;
        statsHeaderY  = y; y += 14;
        statsListBaseY = y; y += n * 16 + 2;
        addBtnY       = y; y += 21;
        boxBottom     = y;
        buttonsY      = Math.max(y + 4, this.height - 26);

        // Sidebar title edit box
        EditBox titleBox = new EditBox(font, innerX, titleEditY, innerW, 16,
                Component.literal("Sidebar Title"));
        titleBox.setMaxLength(32);
        titleBox.setHint(Component.literal("Sidebar Title..."));
        titleBox.setValue(tempTitle);
        titleBox.setResponder(t -> tempTitle = t);
        addRenderableWidget(titleBox);

        // Scale slider (0.1 – 3.0)
        double scaleVal = (tempScale - 0.1) / 2.9;
        addRenderableWidget(new AbstractSliderButton(innerX, scaleSliderY, innerW, 16,
                Component.literal("Scale: " + String.format("%.1f", (double) tempScale) + "x"), scaleVal) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Scale: " + String.format("%.2f", 0.1 + this.value * 2.9) + "x"));
            }
            @Override protected void applyValue() { tempScale = (float)(0.1 + this.value * 2.9); }
        });

        // Text scale slider (0.5 – 2.0)
        double textScaleVal = (tempTextScale - 0.5) / 1.5;
        addRenderableWidget(new AbstractSliderButton(innerX, textScaleSliderY, innerW, 16,
                Component.literal("Text: " + String.format("%.1f", (double) tempTextScale) + "x"), textScaleVal) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Text: " + String.format("%.2f", 0.5 + this.value * 1.5) + "x"));
            }
            @Override protected void applyValue() { tempTextScale = (float)(0.5 + this.value * 1.5); }
        });

        // Column padding slider (0–20)
        double colPadVal = tempColPad / 20.0;
        addRenderableWidget(new AbstractSliderButton(innerX, colPadSliderY, innerW, 16,
                Component.literal("Col Pad: " + tempColPad), colPadVal) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Col Pad: " + (int) Math.round(this.value * 20)));
            }
            @Override protected void applyValue() { tempColPad = (int) Math.round(this.value * 20); }
        });

        // Font toggle button
        addRenderableWidget(Button.builder(
                Component.literal("Font: " + ("uniform".equals(tempFont) ? "Uniform" : "Default")),
                btn -> {
                    tempFont = "uniform".equals(tempFont) ? "default" : "uniform";
                    btn.setMessage(Component.literal("Font: " + ("uniform".equals(tempFont) ? "Uniform" : "Default")));
                }
        ).bounds(innerX, fontToggleY, innerW, 16).build());

        // Sync interval slider
        double syncVal = (tempSyncInterval - 1) / 59.0;
        addRenderableWidget(new AbstractSliderButton(innerX, syncSliderY, innerW, 16,
                Component.literal("Sync: Every " + tempSyncInterval + "s"), syncVal) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Sync: Every " + (1 + (int) Math.round(this.value * 59)) + "s"));
            }
            @Override protected void applyValue() { tempSyncInterval = 1 + (int) Math.round(this.value * 59); }
        });

        // Warning threshold slider
        addRenderableWidget(new AbstractSliderButton(innerX, warnSliderY, innerW, 16,
                Component.literal("Warning: " + Math.round(tempThresholdWarning * 100) + "%"),
                tempThresholdWarning) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Warning: " + Math.round(this.value * 100) + "%"));
            }
            @Override protected void applyValue() { tempThresholdWarning = (float) this.value; }
        });

        // Critical threshold slider
        addRenderableWidget(new AbstractSliderButton(innerX, critSliderY, innerW, 16,
                Component.literal("Critical: " + Math.round(tempThresholdCritical * 100) + "%"),
                tempThresholdCritical) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Critical: " + Math.round(this.value * 100) + "%"));
            }
            @Override protected void applyValue() { tempThresholdCritical = (float) this.value; }
        });

        // Stat rows — ↑ ↓ × per entry
        if (tempStats != null) {
            for (int i = 0; i < tempStats.size(); i++) {
                final int idx = i;
                int rowY = statsListBaseY + i * 16;
                addRenderableWidget(Button.builder(Component.literal("↑"), btn -> {
                    if (idx > 0) {
                        StatEntry tmp = tempStats.remove(idx);
                        tempStats.add(idx - 1, tmp);
                        rebuildWidgets();
                    }
                }).bounds(innerX, rowY, 14, 14).build());
                addRenderableWidget(Button.builder(Component.literal("↓"), btn -> {
                    if (idx < tempStats.size() - 1) {
                        StatEntry tmp = tempStats.remove(idx);
                        tempStats.add(idx + 1, tmp);
                        rebuildWidgets();
                    }
                }).bounds(innerX + 16, rowY, 14, 14).build());
                addRenderableWidget(Button.builder(Component.literal("×"), btn -> {
                    tempStats.remove(idx);
                    rebuildWidgets();
                }).bounds(innerX + innerW - 14, rowY, 14, 14).build());
            }
        }

        // + Add Stat button
        addRenderableWidget(Button.builder(Component.literal("+ Add Stat"), btn ->
            minecraft.setScreen(new StatPickerScreen(this, entry -> {
                if (tempStats == null) tempStats = new ArrayList<>();
                tempStats.add(entry);
            }))
        ).bounds(innerX, addBtnY, innerW, 16).build());

        // Bottom buttons
        int bx = panelX;
        addRenderableWidget(Button.builder(Component.literal("Set Position"), btn -> {
            applyToConfig();
            NixStatsConfig.save();
            minecraft.setScreen(new PositionScreen(this));
        }).bounds(bx, buttonsY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            applyToConfig();
            NixStatsConfig.save();
            tempLoaded = false;
            minecraft.setScreen(parent);
        }).bounds(bx + 110, buttonsY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            tempLoaded = false;
            minecraft.setScreen(parent);
        }).bounds(bx + 180, buttonsY, 60, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xC0101010);
        g.centeredText(font, title, this.width / 2, 4, 0xFFFFFF);

        // Main box
        g.fill(panelX - 1, boxTop - 1, panelX + panelW + 1, boxBottom + 1, 0xFF555555);
        g.fill(panelX,     boxTop,     panelX + panelW,     boxBottom,     0xFF1E1E1E);

        // Phantom colors section header
        g.centeredText(font, Component.literal("\u2500 Phantom Colors \u2500"),
                panelX + panelW / 2, colorHeaderY + 2, 0x888888);

        // Color swatches — label drawn after super call to stay in front
        int[]    rowColors = { tempColorRested, tempColorWarning, tempColorCritical };
        int[]    rowSels   = { swatchSelRested, swatchSelWarning, swatchSelCritical };

        for (int row = 0; row < 3; row++) {
            int labelY  = colorRowsBaseY + row * 26;
            int swatchY = labelY + 12;
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int sx = innerX + i * 13;
                g.fill(sx - 1, swatchY - 1, sx + 11, swatchY + 11,
                        rowSels[row] == i ? 0xFFFFFFFF : 0xFF444444);
                g.fill(sx, swatchY, sx + 10, swatchY + 10, PRESET_COLORS[i]);
            }
            if (rowSels[row] == -1) {
                int cx = innerX + PRESET_COLORS.length * 13 + 2;
                g.fill(cx - 1, swatchY - 1, cx + 11, swatchY + 11, 0xFFFFFFFF);
                g.fill(cx, swatchY, cx + 10, swatchY + 10, rowColors[row]);
            }
        }

        // Stats section header
        g.centeredText(font, Component.literal("\u2500 Stats \u2500"),
                panelX + panelW / 2, statsHeaderY + 2, 0x888888);

        // Stat row previews — icon + label + value, matching HUD style
        if (tempStats != null) {
            Minecraft mc = Minecraft.getInstance();
            NixStatsConfig tmp = buildTempConfig();
            for (int i = 0; i < tempStats.size(); i++) {
                StatEntry entry = tempStats.get(i);
                int rowY = statsListBaseY + i * 16;

                // Icon (10×10, scaled from 16×16)
                ItemStack icon = StatSidebar.getIcon(entry);
                int iconX = innerX + 32;
                if (!icon.isEmpty()) {
                    g.pose().pushMatrix();
                    g.pose().translate(iconX, rowY + 3);
                    g.pose().scale(10f / 16f, 10f / 16f);
                    g.item(icon, 0, 0);
                    g.pose().popMatrix();
                }

                // Value (right-aligned, before ×)
                int rawValue = StatSidebar.readStatValue(entry, mc);
                String valStr = StatSidebar.formatValue(entry, rawValue);
                int valColor  = StatSidebar.getValueColor(entry, rawValue, tmp);
                int valW = font.width(valStr);
                int valX = innerX + innerW - 16 - valW;
                g.text(font, valStr, valX, rowY + 4, valColor);

                // Label (truncated to fit between icon and value)
                int labelX    = iconX + 12;
                int maxLabelW = valX - labelX - 4;
                String label  = truncateLabel(entry.label, maxLabelW);
                g.text(font, label, labelX, rowY + 4, 0xFFCCCCCC);
            }
        }

        // Live preview — anchored to right edge, vertically centered
        NixStatsConfig tmp = buildTempConfig();
        int previewW = StatSidebar.computeFrameWPx(tmp, minecraft.font, minecraft, tempScale);
        int previewH = Math.round(StatSidebar.frameH(tmp) * tempScale);
        int previewX = this.width - previewW - 8;
        if (previewX > panelX + panelW + 8) {
            int previewY = Math.max(4, (this.height - previewH) / 2);
            g.text(font, "Preview:", previewX, previewY - 12, 0xAAAAAA);
            StatSidebar.render(g, previewX, previewY, tempScale, tmp);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // Color row labels drawn last — on top of swatches and widgets
        for (int row = 0; row < 3; row++) {
            int labelY = colorRowsBaseY + row * 26;
            g.text(font, Component.literal(COLOR_ROW_LABELS[row]), innerX, labelY, 0xFFCCCCCC);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0) {
            double mouseX = event.x(), mouseY = event.y();
            for (int row = 0; row < 3; row++) {
                int swatchY = colorRowsBaseY + row * 26 + 12;
                for (int i = 0; i < PRESET_COLORS.length; i++) {
                    int sx = innerX + i * 13;
                    if (mouseX >= sx && mouseX < sx + 10 && mouseY >= swatchY && mouseY < swatchY + 10) {
                        applySwatchClick(row, i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    private void applySwatchClick(int row, int idx) {
        int color = PRESET_COLORS[idx];
        switch (row) {
            case 0 -> { tempColorRested   = color; swatchSelRested   = idx; }
            case 1 -> { tempColorWarning  = color; swatchSelWarning  = idx; }
            case 2 -> { tempColorCritical = color; swatchSelCritical = idx; }
        }
    }

    private void applyToConfig() {
        NixStatsConfig cfg = NixStatsConfig.get();
        cfg.sidebarTitle       = tempTitle != null ? tempTitle : "nixStats";
        cfg.scale              = tempScale;
        cfg.textScale          = tempTextScale;
        cfg.font               = tempFont != null ? tempFont : "default";
        cfg.colPad             = tempColPad;
        cfg.syncInterval       = tempSyncInterval;
        cfg.colorRested        = tempColorRested;
        cfg.colorWarning       = tempColorWarning;
        cfg.colorCritical      = tempColorCritical;
        cfg.thresholdWarning   = tempThresholdWarning;
        cfg.thresholdCritical  = tempThresholdCritical;
        cfg.stats              = deepCopy(tempStats);
    }

    private NixStatsConfig buildTempConfig() {
        NixStatsConfig tmp = new NixStatsConfig();
        tmp.sidebarTitle      = tempTitle != null ? tempTitle : "nixStats";
        tmp.scale             = tempScale;
        tmp.textScale         = tempTextScale;
        tmp.font              = tempFont != null ? tempFont : "default";
        tmp.colPad            = tempColPad;
        tmp.syncInterval      = tempSyncInterval;
        tmp.colorRested       = tempColorRested;
        tmp.colorWarning      = tempColorWarning;
        tmp.colorCritical     = tempColorCritical;
        tmp.thresholdWarning  = tempThresholdWarning;
        tmp.thresholdCritical = tempThresholdCritical;
        tmp.stats             = deepCopy(tempStats);
        return tmp;
    }

    private static List<StatEntry> deepCopy(List<StatEntry> src) {
        if (src == null) return new ArrayList<>();
        List<StatEntry> copy = new ArrayList<>();
        for (StatEntry e : src) copy.add(new StatEntry(e.statType, e.targetId, e.label));
        return copy;
    }

    private static int findSwatch(int color) {
        for (int i = 0; i < PRESET_COLORS.length; i++)
            if (PRESET_COLORS[i] == color) return i;
        return -1;
    }

    private String truncateLabel(String label, int maxPx) {
        if (label == null || maxPx <= 0) return "";
        if (font.width(label) <= maxPx) return label;
        String t = label;
        while (!t.isEmpty() && font.width(t + "..") > maxPx)
            t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
