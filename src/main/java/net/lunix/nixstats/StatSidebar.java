package net.lunix.nixstats;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.lunix.nixstats.screen.StatPickerScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class StatSidebar {

    private static final int BORDER    = 1;
    private static final int TITLE_H   = 11;
    private static final int ROW_H     = 16;
    private static final int PAD_L     = 3;
    private static final int ICON_SLOT = 12;   // icons scaled to 12×12
    private static final int ICON_GAP  = 3;
    public static int computeFrameWPx(NixStatsConfig cfg, Font font, Minecraft mc, float scale) {
        float trs      = scale * Math.max(0.1f, cfg.textScale);
        int b          = Math.max(1, Math.round(BORDER * scale));
        int padLPx     = Math.round(PAD_L * scale);
        int iconSlotPx = Math.round(ICON_SLOT * scale);
        int iconGapPx  = Math.round(ICON_GAP * scale);
        int colGapPx   = Math.round(4 * scale);
        int rightPadPx = Math.round(4 * scale);
        int maxLabelPx = 0;
        int maxValuePx = 0;
        if (cfg.stats != null) {
            for (StatEntry entry : cfg.stats) {
                maxLabelPx = Math.max(maxLabelPx, Math.round((font.width(entry.label) + cfg.colPad) * trs));
                int raw = readStatValue(entry, mc);
                maxValuePx = Math.max(maxValuePx, Math.round((font.width(formatValue(entry, raw)) + cfg.colPad) * trs));
            }
        }
        int col1Px = padLPx + iconSlotPx + iconGapPx + maxLabelPx + colGapPx;
        int col2Px = maxValuePx + rightPadPx;
        return Math.max(Math.round(80 * scale), 2 * b + col1Px + col2Px);
    }

    public static int frameH(NixStatsConfig cfg) {
        int n = cfg.stats != null ? cfg.stats.size() : 1;
        return 2 * BORDER + TITLE_H + BORDER + n * ROW_H + 2;
    }

    private static final int COL_BORDER   = 0xFF636363;
    private static final int COL_BG       = 0xFF1E1E1E;
    private static final int COL_TITLE_BG = 0xFF2A2A2A;
    private static final int COL_LABEL    = 0xFFCCCCCC;
    private static final int COL_VALUE    = 0xFFFFFFFF;

    public static void render(GuiGraphicsExtractor g, int x, int y, float scale, NixStatsConfig cfg) {
        Minecraft mc = Minecraft.getInstance();
        Font font = resolveFont(mc, cfg.font);
        float trs = scale * Math.max(0.1f, cfg.textScale);

        int b          = Math.max(1, Math.round(BORDER * scale));
        int th         = Math.round(TITLE_H * scale);
        int rh         = Math.round(ROW_H * scale);
        int padLPx     = Math.round(PAD_L * scale);
        int iconSlotPx = Math.round(ICON_SLOT * scale);
        int iconGapPx  = Math.round(ICON_GAP * scale);
        int colGapPx   = Math.round(4 * scale);
        int rightPadPx = Math.round(4 * scale);
        float iconScale = (ICON_SLOT / 16f) * scale;

        List<StatEntry> stats = cfg.stats;

        // Pre-pass: read all values and compute column widths
        int maxLabelPx = 0, maxValuePx = 0;
        int[] rawValues = null;
        String[] valStrs = null;
        int[] valColors = null;
        if (stats != null && !stats.isEmpty()) {
            rawValues = new int[stats.size()];
            valStrs   = new String[stats.size()];
            valColors = new int[stats.size()];
            for (int i = 0; i < stats.size(); i++) {
                StatEntry e = stats.get(i);
                rawValues[i] = readStatValue(e, mc);
                valStrs[i]   = formatValue(e, rawValues[i]);
                valColors[i] = getValueColor(e, rawValues[i], cfg);
                maxLabelPx = Math.max(maxLabelPx, Math.round((font.width(e.label) + cfg.colPad) * trs));
                maxValuePx = Math.max(maxValuePx, Math.round((font.width(valStrs[i]) + cfg.colPad) * trs));
            }
        }

        int col1Px = padLPx + iconSlotPx + iconGapPx + maxLabelPx + colGapPx;
        int col2Px = maxValuePx + rightPadPx;
        int sw = Math.max(Math.round(80 * scale), 2 * b + col1Px + col2Px);
        int sh = Math.round(frameH(cfg) * scale);

        // Border + background
        g.fill(x, y, x + sw, y + sh, COL_BORDER);
        g.fill(x + b, y + b, x + sw - b, y + sh - b, COL_BG);

        // Title bar
        g.fill(x + b, y + b, x + sw - b, y + b + th, COL_TITLE_BG);

        // Title text (centered)
        String titleStr = cfg.sidebarTitle != null ? cfg.sidebarTitle : "nixStats";
        float tfw = font.width(titleStr) * trs;
        float tfx = x + b + (sw - 2 * b - tfw) / 2f;
        float tfy = y + b + (th - 8f * trs) / 2f;
        renderScaledText(g, font, titleStr, tfx, tfy, trs, COL_VALUE);

        // Separator below title
        int sepY = y + b + th;
        g.fill(x + b, sepY, x + sw - b, sepY + b, COL_BORDER);

        if (stats == null || stats.isEmpty()) return;

        int rowsTopY = sepY + b;
        int textX    = x + b + padLPx + iconSlotPx + iconGapPx;

        for (int i = 0; i < stats.size(); i++) {
            StatEntry entry = stats.get(i);
            int rowY = rowsTopY + i * rh;

            if (i % 2 == 1) g.fill(x + b, rowY, x + sw - b, rowY + rh, 0x0AFFFFFF);

            // Icon (col1)
            ItemStack icon = getIcon(entry);
            if (!icon.isEmpty()) {
                int iconX = x + b + padLPx;
                int iconY = rowY + (rh - iconSlotPx) / 2;
                g.pose().pushMatrix();
                g.pose().translate(iconX, iconY);
                g.pose().scale(iconScale, iconScale);
                g.item(icon, 0, 0);
                g.pose().popMatrix();
            }

            float textY = rowY + (rh - 8f * trs) / 2f;

            // Label (col1 — truncation is safety only)
            String labelStr = truncate(font, entry.label, maxLabelPx, trs);
            renderScaledText(g, font, labelStr, textX, textY, trs, COL_LABEL);

            // Value (col2 — right-aligned)
            int valW  = Math.round(font.width(valStrs[i]) * trs);
            float valX = x + sw - b - rightPadPx - valW;
            renderScaledText(g, font, valStrs[i], valX, textY, trs, valColors[i]);
        }
    }

    private static void renderScaledText(GuiGraphicsExtractor g, Font font, String text, float fx, float fy, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(fx, fy);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color);
        g.pose().popMatrix();
    }

    private static Font resolveFont(Minecraft mc, String fontName) {
        // "uniform" font switching placeholder — mc.fontManager is private in 26.1;
        // both options currently use the default font until the API is confirmed.
        return mc.font;
    }

    private static String truncate(Font font, String label, int maxPx, float scale) {
        if (label == null) return "";
        int maxUnscaled = (int)(maxPx / scale);
        if (maxUnscaled <= 0) return "";
        if (font.width(label) <= maxUnscaled) return label;
        String t = label;
        while (!t.isEmpty() && font.width(t + "..") > maxUnscaled)
            t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    public static int readStatValue(StatEntry entry, Minecraft mc) {
        if ("phantom".equals(entry.statType)) return NixStats.getLastRemaining();
        if (mc.player == null) return 0;
        // In singleplayer, read directly from the integrated server for real-time accuracy
        StatsCounter stats = mc.player.getStats();
        var srv = mc.getSingleplayerServer();
        if (srv != null) {
            ServerPlayer sp = srv.getPlayerList().getPlayer(mc.player.getUUID());
            if (sp != null) stats = sp.getStats();
        }
        if (entry.targetId == null) return 0;
        Identifier loc = Identifier.tryParse(entry.targetId);
        if (loc == null) return 0;
        try {
            return switch (entry.statType) {
                case "block_mined" -> {
                    var opt = BuiltInRegistries.BLOCK.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.BLOCK_MINED.get(opt.get())) : 0;
                }
                case "item_used" -> {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ITEM_USED.get(opt.get())) : 0;
                }
                case "item_crafted" -> {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ITEM_CRAFTED.get(opt.get())) : 0;
                }
                case "item_broken" -> {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ITEM_BROKEN.get(opt.get())) : 0;
                }
                case "item_picked_up" -> {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ITEM_PICKED_UP.get(opt.get())) : 0;
                }
                case "item_dropped" -> {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ITEM_DROPPED.get(opt.get())) : 0;
                }
                case "entity_killed" -> {
                    var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ENTITY_KILLED.get(opt.get())) : 0;
                }
                case "entity_killed_by" -> {
                    var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.ENTITY_KILLED_BY.get(opt.get())) : 0;
                }
                case "custom" -> {
                    var opt = BuiltInRegistries.CUSTOM_STAT.getOptional(loc);
                    yield opt.isPresent() ? stats.getValue(Stats.CUSTOM.get(opt.get())) : 0;
                }
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatValue(StatEntry entry, int raw) {
        if ("phantom".equals(entry.statType)) {
            if (raw <= 0) return "0:00";
            int secs = raw / 20;
            return String.format("%d:%02d", secs / 60, secs % 60);
        }
        if ("custom".equals(entry.statType) && StatPickerScreen.isTimeStat(entry.targetId)) {
            if (raw <= 0) return "0:00:00";
            int secs = raw / 20;
            return String.format("%d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60);
        }
        if (raw >= 1_000_000_000) return String.format("%.1fB", raw / 1_000_000_000.0);
        if (raw >= 1_000_000)     return String.format("%.1fM", raw / 1_000_000.0);
        if (raw >= 1_000)         return String.format("%.1fK", raw / 1_000.0);
        return String.valueOf(raw);
    }

    public static int getValueColor(StatEntry entry, int raw, NixStatsConfig cfg) {
        if (!"phantom".equals(entry.statType)) return COL_VALUE;
        float fraction = Math.min(1f, (float) raw / NixStats.PHANTOM_THRESHOLD);
        if (fraction > cfg.thresholdWarning)  return cfg.colorRested;
        if (fraction > cfg.thresholdCritical) return cfg.colorWarning;
        return cfg.colorCritical;
    }

    public static ItemStack getIcon(StatEntry entry) {
        if ("phantom".equals(entry.statType)) return new ItemStack(Items.PHANTOM_MEMBRANE);
        if ("entity_killed".equals(entry.statType) || "entity_killed_by".equals(entry.statType))
            return resolveSpawnEgg(entry.targetId);
        if ("custom".equals(entry.statType)) return new ItemStack(Items.PAPER);
        return resolveItem(entry.targetId);
    }

    private static ItemStack resolveItem(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(loc)
                .filter(i -> i != Items.AIR)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack resolveSpawnEgg(String entityId) {
        if (entityId == null) return new ItemStack(Items.SKELETON_SKULL);
        Identifier id = Identifier.tryParse(entityId);
        if (id != null) {
            Identifier eggId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
            var opt = BuiltInRegistries.ITEM.getOptional(eggId);
            if (opt.isPresent() && opt.get() != Items.AIR) return new ItemStack(opt.get());
        }
        return new ItemStack(Items.SKELETON_SKULL);
    }

    public static HudElement create() {
        return new HudElement() {
            @Override
            public void extractRenderState(GuiGraphicsExtractor ext, DeltaTracker dt) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.options.hideGui) return;

                NixStatsConfig cfg = NixStatsConfig.get();
                if (cfg.stats == null || cfg.stats.isEmpty()) return;

                float scale = cfg.scale;
                int sw = computeFrameWPx(cfg, mc.font, mc, scale);
                int sh = Math.round(frameH(cfg) * scale);

                int x = cfg.posX < 0 ? ext.guiWidth() - sw - 4 : cfg.posX;
                int y = cfg.posY < 0 ? 4 : cfg.posY;

                StatSidebar.render(ext, x, y, scale, cfg);
            }
        };
    }
}
