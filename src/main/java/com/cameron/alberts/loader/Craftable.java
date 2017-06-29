package com.cameron.alberts.loader;

import java.util.List;

/**
 * Used to register item/block recipes through the {@link ResourceLoader} when a
 * class that implements {@link Craftable} is also annotated with {@link AutoRegister}.
 */
public interface Craftable {
    List<Recipe> recipes();
}
