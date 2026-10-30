package com.poris.elegies.fingertip.core;

import com.poris.elegies.fingertip.api.inventory.SourceAddress;
import com.poris.elegies.fingertip.api.inventory.SourceAccess;
/**
 * The 指尖检索法 (Fingertip search; the user named it on 2026-10-27: "从小到大找到就用"): matching, nonempty sources in
 * address order, the first one in storage is used. The off hand only reports that the item sits there; observed slots
 * (armour, cursor) never count. Supplies and equipment alike: an equipment identity found twice is not special, both
 * sides reach the same lowest-numbered copy (the user: "就算重复了 计算到的也应该一样才对").
 */
public final class SourceResolver {
    public enum Status { READY, MISSING, OFFHAND }
    public record Result(Status status, SourceAddress address) {
        public static final Result MISSING = new Result(Status.MISSING, null);
        public static final Result OFFHAND = new Result(Status.OFFHAND, null);
        public boolean ready() { return status == Status.READY; }
        public int slot() { return address == null ? -1 : address.legacySlot(); }
    }

    private SourceAddress storage;
    private boolean offhand;

    /** False means final: the first match in storage. */
    public boolean accept(SourceAddress address, SourceAccess access) {
        if (access == SourceAccess.STORAGE) {
            storage = address;
            return false;
        }
        if (access == SourceAccess.OFFHAND) offhand = true;
        return true;
    }
    public Result result() {
        if (storage != null) return new Result(Status.READY, storage);
        return offhand ? Result.OFFHAND : Result.MISSING;
    }
}
