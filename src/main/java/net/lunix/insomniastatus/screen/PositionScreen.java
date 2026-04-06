package net.lunix.insomniastatus.screen;

import net.lunix.insomniastatus.InsomniaConfig;
import net.lunix.insomniastatus.InsomniaHud;
import net.lunix.insomniastatus.InsomniaStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PositionScreen extends Screen {

    private final Screen parent;

    private int     hudX;
    private int     hudY;
    private boolean dragging = false;
    private double  dragOffX;
    private double  dragOffY;

    public PositionScreen(Screen parent) {
        super(Component.literal("Set Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        InsomniaConfig cfg = InsomniaConfig.get();
        hudX = cfg.posX < 0 ? this.width  / 2 + 91 + 3 : cfg.posX;
        hudY = cfg.posY < 0 ? this.height - 2 - Math.round(InsomniaHud.frameH(cfg.layout) * cfg.scale) : cfg.posY;

        addRenderableWidget(Button.builder(Component.literal("Reset Position"), btn -> {
            InsomniaConfig cfg2 = InsomniaConfig.get();
            hudX = this.width  / 2 + 91 + 3;
            hudY = this.height - 2 - Math.round(InsomniaHud.frameH(cfg2.layout) * cfg2.scale);
        }).bounds(this.width / 2 - 116, this.height - 26, 110, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            InsomniaConfig cfg2 = InsomniaConfig.get();
            cfg2.posX = hudX;
            cfg2.posY = hudY;
            InsomniaConfig.save();
            minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Dark strip behind hint text for readability, no full-screen overlay
        g.fill(0, 0, this.width, 18, 0xA0000000);
        g.centeredText(font, Component.literal("Drag the frame to reposition  |  ESC to cancel"),
                this.width / 2, 5, 0xFFFFFF);

        InsomniaConfig cfg = InsomniaConfig.get();
        InsomniaHud.render(g, hudX, hudY, cfg.scale, InsomniaStatus.getLastRemaining(), cfg);

        // Hover highlight
        int fw = Math.round(InsomniaHud.frameW(cfg.layout) * cfg.scale);
        int fh = Math.round(InsomniaHud.frameH(cfg.layout) * cfg.scale);
        if (mouseX >= hudX && mouseX < hudX + fw && mouseY >= hudY && mouseY < hudY + fh) {
            g.fill(hudX, hudY, hudX + fw, hudY + fh, 0x30FFFFFF);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed && event.button() == 0) {
            InsomniaConfig cfg = InsomniaConfig.get();
            int fw = Math.round(InsomniaHud.frameW(cfg.layout) * cfg.scale);
            int fh = Math.round(InsomniaHud.frameH(cfg.layout) * cfg.scale);
            double mx = event.x(), my = event.y();
            if (mx >= hudX && mx < hudX + fw && my >= hudY && my < hudY + fh) {
                dragging = true;
                dragOffX = mx - hudX;
                dragOffY = my - hudY;
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            InsomniaConfig cfg = InsomniaConfig.get();
            int fw = Math.round(InsomniaHud.frameW(cfg.layout) * cfg.scale);
            int fh = Math.round(InsomniaHud.frameH(cfg.layout) * cfg.scale);
            hudX = Math.max(0, Math.min(this.width  - fw, (int)(event.x() - dragOffX)));
            hudY = Math.max(0, Math.min(this.height - fh, (int)(event.y() - dragOffY)));
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
