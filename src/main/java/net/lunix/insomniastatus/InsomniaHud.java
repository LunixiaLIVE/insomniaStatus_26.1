package net.lunix.insomniastatus;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InsomniaHud {

    // Each section is one hotbar-slot-sized square at 1× scale
    public static final int CELL_SIZE    = 20;
    private static final int BORDER      = 2;
    private static final int TEXT_CELL_W = 44; // width of text cell at 1× (inner = 40px)

    // "vertical"   → 20  × 38  (icon top, bar bottom)
    // "horizontal" → 38  × 20  (icon left, bar right)
    // "text"       → 62  × 20  (icon left, seconds text right)
    public static int frameW(String layout) {
        return switch (layout != null ? layout : "vertical") {
            case "horizontal" -> CELL_SIZE * 2 - BORDER;
            case "text"       -> CELL_SIZE + TEXT_CELL_W - BORDER;
            default           -> CELL_SIZE;
        };
    }
    public static int frameH(String layout) {
        return "vertical".equals(layout) ? CELL_SIZE * 2 - BORDER : CELL_SIZE;
    }

    private static final int COL_BORDER = 0xFF636363;
    private static final int COL_BG     = 0xFF1E1E1E;
    private static final int COL_BAR_BG = 0xFF2A2A2A;

    public static void render(GuiGraphicsExtractor g, int x, int y, float scale, int remaining, InsomniaConfig cfg) {
        String layout = cfg.layout != null ? cfg.layout : "vertical";
        boolean isVertical = "vertical".equals(layout);
        boolean isText     = "text".equals(layout);

        int cs    = Math.round(CELL_SIZE * scale);
        int b     = Math.max(1, Math.round(BORDER * scale));
        int inner = cs - b * 2;

        int fw = Math.round(frameW(layout) * scale);
        int fh = Math.round(frameH(layout) * scale);

        // Outer border + background
        g.fill(x, y, x + fw, y + fh, COL_BORDER);
        g.fill(x + b, y + b, x + fw - b, y + fh - b, COL_BG);

        // Divider
        if (isVertical) {
            g.fill(x, y + cs - b, x + fw, y + cs, COL_BORDER);
        } else {
            g.fill(x + cs - b, y, x + cs, y + fh, COL_BORDER);
        }

        // Cell 1 — icon (top or left), centered within its cell
        int iconX = x + b + Math.max(0, (inner - 16) / 2);
        int iconY = y + b + Math.max(0, (inner - 16) / 2);
        g.item(resolveIcon(remaining > 0, cfg), iconX, iconY);

        // Bar color shared by both bar and text modes
        float fraction = Math.min(1f, (float) remaining / InsomniaStatus.PHANTOM_THRESHOLD);
        int barColor = fraction > cfg.thresholdWarning  ? cfg.colorRested
                     : fraction > cfg.thresholdCritical ? cfg.colorWarning
                     : cfg.colorCritical;

        if (isText) {
            // Text cell — remaining time, colored by threshold, scaled to match HUD scale
            int secs = remaining / 20;
            String text = secs >= 60
                ? String.format("%d:%02d", secs / 60, secs % 60)
                : secs + "s";
            Font font = Minecraft.getInstance().font;
            int textCellW = fw - cs - b;
            float textW = font.width(text) * scale;
            float textH = 8f * scale;
            float textX = x + cs + Math.max(0f, (textCellW - textW) / 2f);
            float textY = y + b + Math.max(0f, (inner - textH) / 2f);
            g.pose().pushMatrix();
            g.pose().translate(textX, textY);
            g.pose().scale(scale, scale);
            g.text(font, text, 0, 0, barColor);
            g.pose().popMatrix();
        } else {
            // Bar cell (bottom or right)
            int barX = isVertical ? x + b  : x + cs;
            int barY = isVertical ? y + cs : y + b;
            g.fill(barX, barY, barX + inner, barY + inner, COL_BAR_BG);
            int barFillH = Math.round(fraction * inner);
            if (barFillH > 0) {
                g.fill(barX, barY + inner - barFillH, barX + inner, barY + inner, barColor);
            }
        }
    }

    public static ItemStack resolveIcon(boolean rested, InsomniaConfig cfg) {
        String id = rested ? cfg.restedIcon : cfg.insomniaIcon;
        if (id != null && !id.isEmpty()) {
            Identifier loc = Identifier.tryParse(id);
            if (loc != null) {
                var opt = BuiltInRegistries.ITEM.getOptional(loc);
                if (opt.isPresent() && opt.get() != Items.AIR) return new ItemStack(opt.get());
            }
        }
        return new ItemStack(rested ? Items.WHITE_BED : Items.PHANTOM_MEMBRANE);
    }

    public static HudElement create() {
        return new HudElement() {
            @Override
            public void extractRenderState(GuiGraphicsExtractor ext, DeltaTracker dt) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.options.hideGui) return;

                InsomniaConfig cfg = InsomniaConfig.get();
                int remaining = InsomniaStatus.getLastRemaining();

                int x = cfg.posX < 0 ? ext.guiWidth()  / 2 + 91 + 3 : cfg.posX;
                int y = cfg.posY < 0 ? ext.guiHeight() - 2 - Math.round(frameH(cfg.layout) * cfg.scale) : cfg.posY;

                InsomniaHud.render(ext, x, y, cfg.scale, remaining, cfg);
            }
        };
    }
}
