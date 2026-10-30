package com.poris.elegies.fingertip.api;

import java.util.Collection;

@FunctionalInterface
public interface ActionProvider { Collection<? extends FingertipAction> actions(); }
