package net.lunix.nixstats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NixStatsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("nixstats.json");

    private static NixStatsConfig instance;

    // HUD position (-1 = auto, top-right corner)
    public int posX = -1;
    public int posY = -1;

    // Scale: 0.1 (min) to 3.0 (max)
    public float scale = 1.0f;

    // Text scale relative to HUD scale: 0.5 to 2.0
    public float textScale = 1.0f;

    // Extra padding added to each column's content width (0–20 base units)
    public int colPad = 2;

    // Font: "default" or "uniform"
    public String font = "default";

    // Sidebar title text
    public String sidebarTitle = "nixStats";

    // Phantom timer colors (ARGB)
    public int colorRested   = 0xFF55FF55;
    public int colorWarning  = 0xFFFFFF55;
    public int colorCritical = 0xFFFF5555;

    // Fraction of PHANTOM_THRESHOLD remaining at which state switches
    public float thresholdWarning  = 0.5f;
    public float thresholdCritical = 0.2f;

    // Multiplayer stat sync interval in seconds (1–10)
    public int syncInterval = 5;

    // Tracked stats displayed in the sidebar (in order)
    public List<StatEntry> stats = new ArrayList<>();

    public static NixStatsConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(r, NixStatsConfig.class);
                if (instance == null)              instance = defaultConfig();
                if (instance.stats == null)        instance.stats = new ArrayList<>();
                if (instance.sidebarTitle == null) instance.sidebarTitle = "nixStats";
                if (instance.font == null)         instance.font = "default";
                if (instance.textScale <= 0)       instance.textScale = 1.0f;
                if (instance.colPad < 0)           instance.colPad = 2;
                if (instance.stats.isEmpty())      instance.stats.add(StatEntry.phantom());
            } catch (Exception e) {
                instance = defaultConfig();
            }
        } else {
            instance = defaultConfig();
        }
        save();
    }

    private static NixStatsConfig defaultConfig() {
        NixStatsConfig c = new NixStatsConfig();
        c.stats.add(StatEntry.phantom());
        return c;
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception ignored) {}
    }
}
