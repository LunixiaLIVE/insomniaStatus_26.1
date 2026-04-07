package net.lunix.nixstats;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.lunix.nixstats.screen.NixStatsConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.lwjgl.glfw.GLFW;

public class NixStats implements ClientModInitializer {

    public static final int PHANTOM_THRESHOLD = 72000;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("nixstats", "nixstats_hud");

    private static int lastRemaining = PHANTOM_THRESHOLD;
    private static int syncTick      = 0;
    private static KeyMapping openConfigKey;

    public static int getLastRemaining() {
        return lastRemaining;
    }

    @Override
    public void onInitializeClient() {
        NixStatsConfig.load();

        // Keybind — unbound by default, configurable in Controls
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("nixstats", "config")
        );
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.nixstats.config",
            GLFW.GLFW_KEY_UNKNOWN,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Phantom timer: read server-side in singleplayer for tick-accurate value
            MinecraftServer srv = client.getSingleplayerServer();
            if (srv != null) {
                ServerPlayer sp = srv.getPlayerList().getPlayer(client.player.getUUID());
                int ticksSinceRest = sp != null
                    ? sp.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST))
                    : client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                lastRemaining = Math.max(0, PHANTOM_THRESHOLD - ticksSinceRest);
            } else {
                int ticksSinceRest = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                lastRemaining = Math.max(0, PHANTOM_THRESHOLD - ticksSinceRest);
            }

            // Periodically push REQUEST_STATS to keep all client-side stats fresh.
            // Works for both singleplayer and multiplayer — server responds with a
            // full stats packet that updates mc.player.getStats() for the sidebar.
            if (client.getConnection() != null &&
                ++syncTick >= NixStatsConfig.get().syncInterval * 20) {
                syncTick = 0;
                client.getConnection().send(
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS)
                );
            }

            if (openConfigKey.consumeClick()) {
                client.execute(() -> client.setScreen(new NixStatsConfigScreen(null)));
            }
        });

        // Register HUD element
        HudElementRegistry.addLast(ID, StatSidebar.create());

        // Register /nixstats config command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("nixstats")
                .then(ClientCommands.literal("config")
                    .executes(ctx -> {
                        Minecraft.getInstance().execute(() ->
                            Minecraft.getInstance().setScreen(new NixStatsConfigScreen(null))
                        );
                        return 1;
                    })))
        );
    }
}
