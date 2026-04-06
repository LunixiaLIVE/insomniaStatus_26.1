package net.lunix.insomniastatus.screen;

import net.lunix.insomniastatus.InsomniaConfig;
import net.lunix.insomniastatus.InsomniaHud;
import net.lunix.insomniastatus.InsomniaStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class InsomniaConfigScreen extends Screen {

    private static final int[] PRESET_COLORS = {
        0xFF55FF55, 0xFF00AA00, 0xFF55FFFF, 0xFF00AAAA,
        0xFF5555FF, 0xFF0000AA, 0xFFFF55FF, 0xFFAA00AA,
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFFFFFFFF
    };

    private static boolean tempLoaded = false;
    private static int     tempColorRested;
    private static int     tempColorWarning;
    private static int     tempColorCritical;
    private static float   tempThresholdWarning;
    private static float   tempThresholdCritical;
    private static float   tempScale;
    private static String  tempRestedIcon;
    private static String  tempInsomniaIcon;
    private static String  tempLayout;
    private static int     swatchSelRested;
    private static int     swatchSelWarning;
    private static int     swatchSelCritical;

    private final Screen parent;

    private int panelX;
    private int panelW;
    private int titleY;
    private int colorRowsBaseY;
    private int warnSliderY;
    private int critSliderY;
    private int scaleSliderY;
    private int layoutY;
    private int iconRestedY;
    private int iconInsomniaY;
    private int buttonsY;

    public InsomniaConfigScreen(Screen parent) {
        super(Component.literal("Insomnia Status Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!tempLoaded) {
            InsomniaConfig cfg = InsomniaConfig.get();
            tempColorRested       = cfg.colorRested;
            tempColorWarning      = cfg.colorWarning;
            tempColorCritical     = cfg.colorCritical;
            tempThresholdWarning  = cfg.thresholdWarning;
            tempThresholdCritical = cfg.thresholdCritical;
            tempScale             = cfg.scale;
            tempRestedIcon        = cfg.restedIcon;
            tempInsomniaIcon      = cfg.insomniaIcon;
            tempLayout            = cfg.layout != null ? cfg.layout : "vertical";
            swatchSelRested       = findSwatch(tempColorRested);
            swatchSelWarning      = findSwatch(tempColorWarning);
            swatchSelCritical     = findSwatch(tempColorCritical);
            tempLoaded = true;
        }

        panelW = 280;
        panelX = (this.width - panelW) / 2;

        int y = Math.max(10, (this.height - 272) / 2);

        titleY         = y; y += 20;
        colorRowsBaseY = y; y += 3 * 16 + 4;
        y += 8;
        warnSliderY    = y; y += 20;
        critSliderY    = y; y += 20;
        y += 8;
        scaleSliderY   = y; y += 20;
        y += 8;
        layoutY        = y; y += 20;
        y += 8;
        iconRestedY    = y; y += 20;
        iconInsomniaY  = y; y += 20;
        y += 8;
        buttonsY       = y;

        int sliderX = panelX + 90;
        int sliderW = panelW - 90;

        addRenderableWidget(new AbstractSliderButton(sliderX, warnSliderY, sliderW, 16,
                Component.literal("Warning: " + Math.round(tempThresholdWarning * 100) + "%"),
                tempThresholdWarning) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Warning: " + Math.round(this.value * 100) + "%"));
            }
            @Override protected void applyValue() { tempThresholdWarning = (float) this.value; }
        });

        addRenderableWidget(new AbstractSliderButton(sliderX, critSliderY, sliderW, 16,
                Component.literal("Critical: " + Math.round(tempThresholdCritical * 100) + "%"),
                tempThresholdCritical) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Critical: " + Math.round(this.value * 100) + "%"));
            }
            @Override protected void applyValue() { tempThresholdCritical = (float) this.value; }
        });

        double scaleVal = (tempScale - 1.0) / 2.0;
        addRenderableWidget(new AbstractSliderButton(sliderX, scaleSliderY, sliderW, 16,
                Component.literal("Scale: " + String.format("%.1f", (double) tempScale) + "x"),
                scaleVal) {
            @Override protected void updateMessage() {
                setMessage(Component.literal("Scale: " + String.format("%.1f", 1.0 + this.value * 2.0) + "x"));
            }
            @Override protected void applyValue() { tempScale = (float)(1.0 + this.value * 2.0); }
        });

        // Layout cycle button: Vertical → Horizontal → Text → Vertical
        addRenderableWidget(Button.builder(
            Component.literal(layoutLabel(tempLayout)),
            btn -> {
                tempLayout = nextLayout(tempLayout);
                btn.setMessage(Component.literal(layoutLabel(tempLayout)));
            }
        ).bounds(sliderX, layoutY, sliderW, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Change"), btn ->
            minecraft.setScreen(new ItemPickerScreen(this, item -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) tempRestedIcon = id.toString();
            }))
        ).bounds(panelX + panelW - 64, iconRestedY, 60, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Change"), btn ->
            minecraft.setScreen(new ItemPickerScreen(this, item -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) tempInsomniaIcon = id.toString();
            }))
        ).bounds(panelX + panelW - 64, iconInsomniaY, 60, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Set Position"), btn -> {
            applyToConfig();
            InsomniaConfig.save();
            minecraft.setScreen(new PositionScreen(this));
        }).bounds(panelX, buttonsY, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            applyToConfig();
            InsomniaConfig.save();
            tempLoaded = false;
            minecraft.setScreen(parent);
        }).bounds(panelX + 110, buttonsY, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            tempLoaded = false;
            minecraft.setScreen(parent);
        }).bounds(panelX + 180, buttonsY, 60, 20).build());
    }

    private static String nextLayout(String current) {
        return switch (current) {
            case "horizontal" -> "text";
            case "text"       -> "vertical";
            default           -> "horizontal";
        };
    }

    private static String layoutLabel(String layout) {
        return switch (layout) {
            case "horizontal" -> "Horizontal";
            case "text"       -> "Text";
            default           -> "Vertical";
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xC0101010);

        g.centeredText(font, title, this.width / 2, titleY + 4, 0xFFFFFF);

        String[] rowLabels = { "Rested:", "Warning:", "Critical:" };
        int[]    rowColors = { tempColorRested, tempColorWarning, tempColorCritical };
        int[]    rowSels   = { swatchSelRested, swatchSelWarning, swatchSelCritical };
        int      swatchX   = panelX + 60;

        for (int row = 0; row < 3; row++) {
            int rowY = colorRowsBaseY + row * 16;
            g.text(font, rowLabels[row], panelX, rowY + 2, 0xCCCCCC);
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int sx = swatchX + i * 13;
                g.fill(sx - 1, rowY - 1, sx + 11, rowY + 11, rowSels[row] == i ? 0xFFFFFFFF : 0xFF444444);
                g.fill(sx, rowY, sx + 10, rowY + 10, PRESET_COLORS[i]);
            }
            if (rowSels[row] == -1) {
                int cx = swatchX + PRESET_COLORS.length * 13 + 2;
                g.fill(cx - 1, rowY - 1, cx + 11, rowY + 11, 0xFFFFFFFF);
                g.fill(cx, rowY, cx + 10, rowY + 10, rowColors[row]);
            }
        }

        g.text(font, "Threshold:", panelX, warnSliderY + 2, 0xAAAAAA);
        g.text(font, "Scale:", panelX, scaleSliderY + 2, 0xAAAAAA);
        g.text(font, "Layout:", panelX, layoutY + 2, 0xAAAAAA);

        g.text(font, "Rested:", panelX, iconRestedY + 3, 0xCCCCCC);
        InsomniaConfig tmp = buildTempConfig();
        g.item(InsomniaHud.resolveIcon(true, tmp), panelX + 58, iconRestedY);

        g.text(font, "Insomnia:", panelX, iconInsomniaY + 3, 0xCCCCCC);
        g.item(InsomniaHud.resolveIcon(false, tmp), panelX + 58, iconInsomniaY);

        // HUD preview — right of panel, reflects unsaved temp config
        int previewFH = Math.round(InsomniaHud.frameH(tempLayout) * tempScale);
        int previewX  = panelX + panelW + 20;
        int previewY  = Math.max(10, (this.height - previewFH) / 2);
        g.text(font, "Preview:", previewX, previewY - 12, 0xAAAAAA);
        InsomniaHud.render(g, previewX, previewY, tempScale, InsomniaStatus.getLastRemaining(), tmp);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0) {
            double mouseX = event.x(), mouseY = event.y();
            int swatchX = panelX + 60;
            for (int row = 0; row < 3; row++) {
                int rowY = colorRowsBaseY + row * 16;
                for (int i = 0; i < PRESET_COLORS.length; i++) {
                    int sx = swatchX + i * 13;
                    if (mouseX >= sx && mouseX < sx + 10 && mouseY >= rowY && mouseY < rowY + 10) {
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
        InsomniaConfig cfg = InsomniaConfig.get();
        cfg.colorRested       = tempColorRested;
        cfg.colorWarning      = tempColorWarning;
        cfg.colorCritical     = tempColorCritical;
        cfg.thresholdWarning  = tempThresholdWarning;
        cfg.thresholdCritical = tempThresholdCritical;
        cfg.scale             = tempScale;
        cfg.restedIcon        = tempRestedIcon;
        cfg.insomniaIcon      = tempInsomniaIcon;
        cfg.layout            = tempLayout;
    }

    private InsomniaConfig buildTempConfig() {
        InsomniaConfig tmp = new InsomniaConfig();
        tmp.colorRested       = tempColorRested;
        tmp.colorWarning      = tempColorWarning;
        tmp.colorCritical     = tempColorCritical;
        tmp.thresholdWarning  = tempThresholdWarning;
        tmp.thresholdCritical = tempThresholdCritical;
        tmp.scale             = tempScale;
        tmp.restedIcon        = tempRestedIcon;
        tmp.insomniaIcon      = tempInsomniaIcon;
        tmp.layout            = tempLayout;
        return tmp;
    }

    private static int findSwatch(int color) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == color) return i;
        }
        return -1;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
