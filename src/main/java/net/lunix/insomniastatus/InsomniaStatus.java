package net.lunix.insomniastatus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.lunix.insomniastatus.screen.InsomniaConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import org.lwjgl.glfw.GLFW;

public class InsomniaStatus implements ClientModInitializer {

    public static final int PHANTOM_THRESHOLD = 72000;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("insomniastatus", "insomnia_hud");

    private static int lastRemaining = PHANTOM_THRESHOLD;
    private static KeyMapping openConfigKey;

    public static int getLastRemaining() {
        return lastRemaining;
    }

    @Override
    public void onInitializeClient() {
        InsomniaConfig.load();

        // Keybind — unbound by default, configurable in Controls
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("insomniastatus", "config")
        );
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.insomniastatus.config",
            GLFW.GLFW_KEY_UNKNOWN,
            category
        ));

        // Tick: update remaining ticks and keep the MobEffect in sync
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Client-side StatsCounter is only synced when the stats screen is opened.
            // Read directly from the integrated server's player in singleplayer,
            // fall back to client-side stats in multiplayer.
            int ticksSinceRest;
            MinecraftServer srv = client.getSingleplayerServer();
            if (srv != null) {
                ServerPlayer sp = srv.getPlayerList().getPlayer(client.player.getUUID());
                ticksSinceRest = sp != null
                    ? sp.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST))
                    : client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            } else {
                ticksSinceRest = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            }
            lastRemaining = Math.max(0, PHANTOM_THRESHOLD - ticksSinceRest);

            if (openConfigKey.consumeClick()) {
                client.execute(() -> client.setScreen(new InsomniaConfigScreen(null)));
            }
        });

        // Register HUD element
        HudElementRegistry.addLast(ID, InsomniaHud.create());

        // Register /insomnia config command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("insomnia")
                .then(ClientCommands.literal("config")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().setScreen(new InsomniaConfigScreen(null))
                        );
                        return 1;
                    })))
        );
    }
}
