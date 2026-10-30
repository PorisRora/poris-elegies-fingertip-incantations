package com.poris.elegies.fingertip.core;

import com.poris.elegies.fingertip.api.inventory.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static com.poris.elegies.fingertip.core.SourceResolver.*;
import static org.junit.jupiter.api.Assertions.*;

/** The 指尖检索法: supplies and equipment alike stop at the first storage match; duplicates are not special. */
class SourceSearchTest {
    private static final SourceAddress BAG = new SourceAddress("fixture:bag", 2);
    @Test void emptySearchHasNoSource() {
        assertEquals(Result.MISSING,new SourceResolver().result());
    }
    @Test void searchStopsAtFirstEligibleAddress() {
        var search=new SourceResolver();
        assertFalse(search.accept(SourceAddress.vanilla(2),SourceAccess.STORAGE));
        assertEquals(new Result(Status.READY,SourceAddress.vanilla(2)),search.result());
    }
    @Test void observedCopiesCannotBeSelected() {
        var search=new SourceResolver();
        assertTrue(search.accept(SourceAddress.vanilla(36),SourceAccess.OBSERVED));
        assertTrue(search.accept(SourceAddress.vanilla(41),SourceAccess.OBSERVED));
        assertEquals(Result.MISSING,search.result());
    }
    @Test void observedCopyDoesNotHideStorage() {
        var search=new SourceResolver();
        assertTrue(search.accept(SourceAddress.vanilla(36),SourceAccess.OBSERVED));
        assertFalse(search.accept(BAG,SourceAccess.STORAGE));
        assertEquals(new Result(Status.READY,BAG),search.result());
    }
    @Test void offhandDoesNotPreventFindingExternalStorage() {
        var search=new SourceResolver();
        assertTrue(search.accept(SourceAddress.vanilla(40),SourceAccess.OFFHAND));
        assertFalse(search.accept(BAG,SourceAccess.STORAGE));
        assertEquals(new Result(Status.READY,BAG),search.result());
    }
    @Test void offhandOnlyIsUnavailable() {
        var search=new SourceResolver();search.accept(SourceAddress.vanilla(40),SourceAccess.OFFHAND);
        assertEquals(Result.OFFHAND,search.result());
    }
    @Test void externalAccessDoesNotDependOnVanillaSlotRanges() {
        var far=new SourceAddress("fixture:bag",250);var search=new SourceResolver();
        assertFalse(search.accept(far,SourceAccess.STORAGE));
        assertEquals(SourceAddress.EXTERNAL,search.result().slot());
    }
    @Test void providerOrderIsVanillaThenIdThenSlot() {
        var sources=new ArrayList<>(List.of(new SourceAddress("z:bag",0),new SourceAddress("a:bag",5),
                SourceAddress.vanilla(35),new SourceAddress("a:bag",2)));
        sources.sort(null);
        assertEquals(List.of(SourceAddress.vanilla(35),new SourceAddress("a:bag",2),
                new SourceAddress("a:bag",5),new SourceAddress("z:bag",0)),sources);
    }
    @Test void addressReuseAndValidationRemainIntact() {
        assertSame(SourceAddress.vanilla(0),SourceAddress.vanilla(0));
        assertSame(SourceAddress.vanilla(63),SourceAddress.vanilla(63));
        assertEquals(new SourceAddress(SourceAddress.VANILLA,64),SourceAddress.vanilla(64));
        assertNotEquals(new SourceAddress("missing:bag",2),SourceAddress.vanilla(2));
        assertThrows(IllegalArgumentException.class,()->SourceAddress.vanilla(-1));
        assertThrows(IllegalArgumentException.class,()->new SourceAddress("bag",2));
    }
}
