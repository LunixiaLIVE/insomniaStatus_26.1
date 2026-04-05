package net.lunix.insomniastatus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InsomniaStatus implements ClientModInitializer {

    private static final int PHANTOM_THRESHOLD = 72000;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("insomniastatus", "insomnia_hud");
    private static final Identifier EFFECT_BG = Identifier.fromNamespaceAndPath("minecraft", "hud/effect_background");

    private static Holder<MobEffect> RESTED_EFFECT;

    @Override
    public void onInitializeClient() {
        // Register a real MobEffect so other mods can detect insomnia state
        MobEffect restedEffect = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath("insomniastatus", "rested"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xF5A623) {}
        );
        RESTED_EFFECT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(restedEffect);

        // Keep client-side effect in sync with insomnia timer each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            int ticksSinceRest = client.player.getStats().getValue(
                Stats.CUSTOM.get(Stats.TIME_SINCE_REST)
            );
            int remaining = PHANTOM_THRESHOLD - ticksSinceRest;

            if (remaining > 0) {
                MobEffectInstance existing = client.player.getEffect(RESTED_EFFECT);
                // Re-apply if missing or duration is off by more than 2 seconds
                if (existing == null || Math.abs(existing.getDuration() - remaining) > 40) {
                    client.player.addEffect(new MobEffectInstance(RESTED_EFFECT, remaining, 0, false, false, false));
                }
            } else {
                if (client.player.hasEffect(RESTED_EFFECT)) {
                    client.player.removeEffect(RESTED_EFFECT);
                }
            }
        });

        // HUD rendering: item icon + countdown timer
        HudElementRegistry.addLast(ID, new HudElement() {
            @Override
            public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null || client.options.hideGui) return;

                int ticksSinceRest = client.player.getStats().getValue(
                    Stats.CUSTOM.get(Stats.TIME_SINCE_REST)
                );

                int remaining = PHANTOM_THRESHOLD - ticksSinceRest;
                ItemStack icon = remaining > 0
                    ? new ItemStack(Items.CAMPFIRE)
                    : new ItemStack(Items.PHANTOM_MEMBRANE);

                int x = extractor.guiWidth() - 25;
                int y = 1;

                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BG, x, y, 24, 24);
                extractor.item(icon, x + 4, y + 4);

                if (remaining > 0) {
                    String timeText = formatTicks(remaining);
                    int textX = x + 12 - client.font.width(timeText) / 2;
                    extractor.text(client.font, timeText, textX, y + 26, 0xFFFFFF);
                }
            }
        });
    }

    private static String formatTicks(int ticks) {
        int totalSeconds = ticks / 20;
        int totalMinutes = totalSeconds / 60;
        int totalHours = totalMinutes / 60;
        int days = totalHours / 24;
        int hours = totalHours % 24;
        int minutes = totalMinutes % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return totalMinutes + "m";
    }
}
