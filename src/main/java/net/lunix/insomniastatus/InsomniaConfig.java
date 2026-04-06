package net.lunix.insomniastatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class InsomniaConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("insomniastatus.json");

    private static InsomniaConfig instance;

    // HUD position (-1 = auto, positioned right of hotbar)
    public int posX = -1;
    public int posY = -1;

    // Scale: 1.0 (default/min) to 3.0 (max)
    public float scale = 1.0f;

    // Bar fill colors (ARGB)
    public int colorRested   = 0xFF55FF55;
    public int colorWarning  = 0xFFFFFF55;
    public int colorCritical = 0xFFFF5555;

    // Fraction of PHANTOM_THRESHOLD remaining at which state switches
    public float thresholdWarning  = 0.5f;
    public float thresholdCritical = 0.2f;

    // Item registry IDs for the HUD icon
    public String restedIcon   = "minecraft:white_bed";
    public String insomniaIcon = "minecraft:phantom_membrane";

    // Layout: "vertical" (icon top, bar bottom), "horizontal" (icon left, bar right),
    //         "text" (icon left, remaining seconds text right)
    public String layout = "vertical";

    public static InsomniaConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(r, InsomniaConfig.class);
                if (instance == null) instance = new InsomniaConfig();
            } catch (Exception e) {
                instance = new InsomniaConfig();
            }
        } else {
            instance = new InsomniaConfig();
        }
        save();
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
