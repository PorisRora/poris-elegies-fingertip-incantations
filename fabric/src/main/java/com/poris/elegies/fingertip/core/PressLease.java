package com.poris.elegies.fingertip.core;

/** Release the action actually pressed, even if its HUD slot is rebound while the key is held. */
public final class PressLease {
    private Runnable release;
    public void begin(Runnable press, Runnable onRelease) {
        end();
        release = onRelease;
        try { press.run(); } catch (RuntimeException | LinkageError error) { end(); throw error; }
    }
    public void end() {
        Runnable old = release;
        release = null;
        if (old != null) old.run();
    }
    public boolean active() { return release != null; }
}
