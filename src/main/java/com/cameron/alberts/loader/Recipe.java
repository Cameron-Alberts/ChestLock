package com.cameron.alberts.loader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.ToString;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.IForgeRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Helper class for building recipes. To make use of this your custom item/block needs
 * to implement {@link Craftable}, be annotated with {@link AutoRegister}, you must
 * have called {@link ResourceLoader#register()}, and then {@link ResourceLoader#registerRecipes()}.
 * The annotation tells the ResourceLoader that you want to register that item/block dynamically,
 * the interface lets the ResourceLoader know that the class has some recipes for it to register,
 * and the call to register will let the ResourceLoader have access to the instances of your custom
 * items/blocks.
 */
@ToString
public class Recipe {
    private final static int ROW_LENGTH = 3;

    private Map<Character, IForgeRegistry<?>> registryMap;
    private Map<Character, Class<?>> classMap;
    private List<String> rows;
    private ItemStack itemStack;

    private Recipe(final Map<Character, IForgeRegistry<?>> registryMap,
                   final Map<Character, Class<?>> classMap,
                   final List<String> rows, final ItemStack itemStack) {
        this.registryMap = registryMap;
        this.classMap = classMap;
        this.rows = rows;
        this.itemStack = itemStack;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public Object[] getRecipe() {
        final List<Object> recipe = Lists.newArrayList();
        recipe.addAll(rows);
        addAllMappings(recipe, registryMap);
        addAllMappings(recipe, classMap);

        return recipe.toArray();
    }

    private <K, V> void addAllMappings(final List<Object> recipe, final Map<K, V> mappings) {
        mappings.entrySet().forEach(m -> recipe.addAll(Arrays.asList(m.getKey(), m.getValue())));
    }

    public static final class Builder {
        private final Map<Character, IForgeRegistry<?>> registryMap;
        private final Map<Character, Class<?>> classMap;
        private final List<String> rows;
        private ItemStack itemStack;

        private Builder() {
            registryMap = Maps.newHashMap();
            classMap = Maps.newHashMap();
            rows = Lists.newArrayList();
        }

        public Builder withItemStack(final ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public Builder withRows(final String row1, final String row2, final String row3) {
            if (!validRowLength(row1) || !validRowLength(row2) || !validRowLength(row3)) {
                throw new IllegalArgumentException("All rows must be of length 3 or less!");
            }

            rows.addAll(Arrays.asList(row1, row2, row3));
            return this;
        }

        /**
         * When building recipes you supply a char and another item/block that the characer
         * maps to.
         */
        public Builder withInstanceMapping(final char charToMap, final IForgeRegistry<?> registryInstance) {
            registryMap.put(charToMap, registryInstance);
            return this;
        }

        /**
         * When building recipes you supply a char and usually another item/block that the characer
         * maps to, but with your own custom items you do not have access to other instances of your
         * custom items, if you're using the {@link ResourceLoader}. To help fix that you can map
         * a char to the Class you want to use.
         */
        public Builder withClassMapping(final char charToMap, final Class clazz) {
            classMap.put(charToMap, clazz);
            return this;
        }

        public Recipe build() {
            return new Recipe(registryMap, classMap, rows, itemStack);
        }

        private boolean validRowLength(final String row) {
            return row.length() <= ROW_LENGTH;
        }
    }
}
