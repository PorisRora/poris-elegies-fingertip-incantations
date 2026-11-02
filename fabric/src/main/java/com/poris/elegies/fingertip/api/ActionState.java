package com.poris.elegies.fingertip.api;

import net.minecraft.network.chat.Component;

/**
 * What a skill slot shows (Magic Action, 2026-10-29: the HUD only shows a skill's state; a press always goes to the
 * skill's owner, which decides and explains a refusal). cooldown is the share still to wait (0 ready, 1 just cast);
 * cooldownSeconds, when the provider knows it, is the time left and is drawn over the icon (preview19.28; 0 = not
 * given). toggle is ON while a toggled skill is running, a summon for example; NONE for a skill that is not one.
 * charges, when not null, is how many presses are left: a recast window or stored charges. A field left out is not
 * drawn, so the state itself says what kind of skill this is. Providers built against older versions keep using the
 * four- and five-value constructors.
 */
public record ActionState(boolean available, float cooldown, boolean lacksResource, Component reason, float cooldownSeconds,
                          Toggle toggle, Charges charges) {
    public enum Toggle { NONE, OFF, ON }

    /**
     * current of max presses left. progress is how far the next one has come back, 0 to 1, for charges that refill;
     * negative when there is nothing to draw, as in a recast window whose time the owning mod shows itself.
     */
    public record Charges(int current, int max, float progress) {
        public Charges {
            max = Math.max(max, 1);
            current = Math.clamp(current, 0, max);
            progress = Float.isFinite(progress) && progress >= 0 ? Math.min(progress, 1) : -1;
        }
        public Charges(int current, int max) { this(current, max, -1); }
    }

    public static final ActionState READY = new ActionState(true, 0, false, Component.empty());
    public ActionState {
        cooldown = Float.isFinite(cooldown) ? Math.clamp(cooldown, 0, 1) : 1;
        reason = reason == null ? Component.empty() : reason;
        cooldownSeconds = Float.isFinite(cooldownSeconds) && cooldownSeconds > 0 && cooldown > 0 ? cooldownSeconds : 0;
        toggle = toggle == null ? Toggle.NONE : toggle;
    }
    public ActionState(boolean available, float cooldown, boolean lacksResource, Component reason, float cooldownSeconds) {
        this(available, cooldown, lacksResource, reason, cooldownSeconds, Toggle.NONE, null);
    }
    public ActionState(boolean available, float cooldown, boolean lacksResource, Component reason) {
        this(available, cooldown, lacksResource, reason, 0);
    }
}
