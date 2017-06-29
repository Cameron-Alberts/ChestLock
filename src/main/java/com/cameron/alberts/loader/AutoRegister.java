package com.cameron.alberts.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate classes that you want to be found by the {@link ResourceLoader}
 * for automatic item registry and item rendering through Forge's GameRegistry
 * class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegister {
}
