package com.poris.elegies.fingertip.client.hud;

import com.poris.elegies.fingertip.client.input.InputRouter;
import com.poris.elegies.fingertip.client.input.OptionalKeyPolicy;
import com.poris.elegies.fingertip.client.input.RawKeyRelay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/**
 * A normal Screen owns the cursor. A middle press opens it and it stays open: a left click on a sector runs it, and the
 * wheel closes, except after the two drops, which can be clicked again and again; a click in the centre, a right click,
 * Esc or another middle press closes it (2026-10-28, user: "轮盘弄成按压点击", "丢弃一个和整组不关闭轮盘可以继续点").
 * Holding the click on drop-one keeps dropping, as holding Q does; drop-stack never repeats, and the other actions,
 * mod ones included, run once. Choosing on release picked a neighbour when the hand slipped, and pressing the scroll
 * wheel scrolled the hotbar.
 * Middle presses and releases go to the wheel while it is open, so they never reach pick block.
 * <p>
 * Drag, the default since 2026-10-29 (user: "滚轮配置里左边加个开关和拖拽吧 默认拖拽模式"; the switch is on the wheel setup
 * page): the wheel opens on the middle press and the middle release runs the action under the cursor once, drops
 * included ("拖拽的话全当触发式的"), or nothing in the centre. Left clicks still work as in the click wheel.
 */
public final class RadialMenuScreen extends Screen {
    private static RadialMenuScreen owner;
    private RadialGeometry geometry;
    private final List<String> actions;
    private int selected = -1;
    private boolean finished;
    /** The drop-one sector held down, or -1; it repeats after 0.3 s, ten a second, while the cursor stays on it. */
    private int holding = -1, heldTicks;
    /** Read when the wheel opens, so changing the setting never turns a held wheel into a click wheel. */
    private final boolean drag;

    private RadialMenuScreen() {
        super(Component.translatable("poris_elegies_fingertip_incantations.wheel.title"));
        actions = List.copyOf(OptionalKeyPolicy.settings().wheel);
        drag = OptionalKeyPolicy.settings().wheelDrag;
    }
    public static boolean route(int button, int action) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || RawKeyRelay.active()) return false;
        var mc = Minecraft.getInstance();
        if (owner != null && (owner.finished || mc.screen != owner)) owner = null;
        if (owner != null) {
            if (action == GLFW.GLFW_PRESS) owner.close(false);
            else if (action == GLFW.GLFW_RELEASE && owner.drag) owner.release();
            return true;
        }
        if (action != GLFW.GLFW_PRESS || !OptionalKeyPolicy.settings().wheelEnabled || !InputRouter.enabled()
                || mc.screen != null || mc.getOverlay() != null || !mc.isWindowActive()
                || mc.player == null || !mc.player.isAlive() || mc.player.isSpectator()) return false;
        InputRouter.releaseAll(); KeyMapping.releaseAll();
        owner = new RadialMenuScreen(); mc.setScreen(owner); return true;
    }
    @Override protected void init() { geometry = RadialGeometry.fit(width, height); selected = -1; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (!minecraft.isWindowActive() || !InputRouter.enabled() || !OptionalKeyPolicy.settings().wheelEnabled
                || minecraft.player == null || !minecraft.player.isAlive() || minecraft.getOverlay() != null) { close(false); return; }
        if (holding < 0) return;
        if (sectorUnderCursor() != holding) { holding = -1; return; }
        if (++heldTicks >= 6 && heldTicks % 2 == 0) RadialActions.repeat(actions.get(holding));
    }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float partial) { g.fill(0, 0, width, height, 0x40100F19); }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        selected = geometry.at(mx, my);
        RadialPainter.draw(g, geometry, actions, selected, false);
        // The two centre lines stay inside the centre circle, shrunk if a language's words are long (2026-10-29).
        g.pose().pushPose();
        try {
            g.pose().translate(geometry.cx(), geometry.cy(), 0);
            RadialPainter.fitted(g, Component.translatable(drag ? "poris_elegies_fingertip_incantations.wheel.release_drag"
                    : "poris_elegies_fingertip_incantations.wheel.release").getString(), -10,
                    geometry.centreWidth(-10, -2) - 6, 1, EditorTheme.accent());
            RadialPainter.fitted(g, Component.translatable(drag ? "poris_elegies_fingertip_incantations.wheel.center_drag"
                    : "poris_elegies_fingertip_incantations.wheel.center").getString(), 5,
                    geometry.centreWidth(5, 13) - 6, 1, EditorTheme.MUTED);
        } finally { g.pose().popPose(); }
        var hint = selected < 0 ? Component.translatable(drag ? "poris_elegies_fingertip_incantations.wheel.cancel_hint_drag"
                        : "poris_elegies_fingertip_incantations.wheel.cancel_hint")
                : RadialActions.label(actions.get(selected)).copy().append(Component.translatable(
                        !RadialActions.available(actions.get(selected)) ? "poris_elegies_fingertip_incantations.wheel.unavailable"
                                : drag ? "poris_elegies_fingertip_incantations.wheel.execute_hint_drag"
                                : "drop".equals(actions.get(selected)) ? "poris_elegies_fingertip_incantations.wheel.repeat_hint"
                                : "drop_stack".equals(actions.get(selected)) ? "poris_elegies_fingertip_incantations.wheel.repeat_stack_hint"
                                : "poris_elegies_fingertip_incantations.wheel.execute_hint"));
        String caption = GuiPixels.ellipsize(font, hint.getString(), width - 24);
        g.drawCenteredString(font, caption, width / 2, Math.min(height - 12, geometry.cy() + geometry.outer() + 7), EditorTheme.TEXT);
    }
    /** The sector under the cursor, or -1; read from the mouse itself where no mouse event gives the position. */
    private int sectorUnderCursor() {
        double x = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double y = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        return geometry.at(x, y);
    }
    /** Drag: the action under the cursor runs once, drops included; nothing runs in the centre. */
    private void release() {
        selected = sectorUnderCursor();
        close(selected >= 0);
    }
    private void close(boolean execute) {
        if (finished) return; finished = true;
        boolean allowed = execute && minecraft.isWindowActive() && InputRouter.enabled() && OptionalKeyPolicy.settings().wheelEnabled;
        KeyMapping.releaseAll();
        minecraft.setScreen(null);
        if (allowed && selected >= 0) RadialActions.execute(actions.get(selected));
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) { close(false); return true; }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        selected = geometry.at(x, y);
        if (selected < 0) { close(false); return true; }
        String id = actions.get(selected);
        if (RadialActions.repeatable(id)) {
            RadialActions.repeat(id);
            if ("drop".equals(id)) { holding = selected; heldTicks = 0; }
        } else close(true);
        return true;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) holding = -1;
        return true;
    }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { return true; }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) close(false);
        return true;
    }
    @Override public void onClose() { close(false); }
    @Override public void removed() { finished = true; super.removed(); }
}
