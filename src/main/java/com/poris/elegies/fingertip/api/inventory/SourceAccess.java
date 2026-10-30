package com.poris.elegies.fingertip.api.inventory;

/** Only STORAGE can supply the hand. OFFHAND also counts toward HUD totals; OBSERVED only detects duplicate identities. */
public enum SourceAccess { STORAGE, OFFHAND, OBSERVED }
