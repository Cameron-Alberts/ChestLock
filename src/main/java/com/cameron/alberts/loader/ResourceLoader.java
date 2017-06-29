package com.cameron.alberts.loader;

import com.cameron.alberts.utils.ResourceNameHelper;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class ResourceLoader {
    private static final String GET_UNLOCALIZED_NAME_METHOD_NAME = "getUnlocalizedName";
    private static final String VARIANT_IN = "inventory";

    private final Map<Class, RegistryEntry> loadedClasses;
    private final Set<Class<?>> classesToLoad;
    private final String modId;

    private boolean registeredCalled;

    /**
     * @param classPath classPath to look for items/blocks annotated with {@link AutoRegister}.
     * @param modId modId to register the items/blocks to.
     */
    public ResourceLoader(final String classPath, final String modId) {
        Reflections reflections = new Reflections(classPath);
        this.classesToLoad = reflections.getTypesAnnotatedWith(AutoRegister.class);
        this.modId = modId;
        this.loadedClasses = Maps.newConcurrentMap();
        this.registeredCalled = false;
    }

    @SuppressWarnings("unchecked")
    public void register() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            log.info("Attempting to register {} classes", classesToLoad.size());
            for (Class clazz : classesToLoad) {
                if (!IForgeRegistryEntry.class.isAssignableFrom(clazz)) {
                    log.error("Class {} does not inherit from IForgeRegistryEntry", clazz.getName());
                    return;
                }

                if (Arrays.stream(clazz.getConstructors()).noneMatch(c -> c.getParameterCount() == 0)) {
                    log.error("Couldn't find a default constructor for class {}", clazz.getName());
                    return;
                }

                IForgeRegistryEntry<?> object = (IForgeRegistryEntry<?>) clazz.getConstructor().newInstance();

                Optional<Method> methodOptional = Arrays.stream(clazz.getMethods()).filter(m ->
                        m.getName().equalsIgnoreCase(GET_UNLOCALIZED_NAME_METHOD_NAME)
                                && m.getParameterCount() == 0).findFirst();

                if (methodOptional.isPresent()) {
                    Method method = methodOptional.get();
                    String name = ResourceNameHelper.getUnlocalizedName((String)method.invoke(object));

                    registerObject(object, modId, name);
                    loadedClasses.put(clazz, new RegistryEntry(object, name, clazz));

                    log.info("Registered {}.class with mod id {} and name {}",
                            clazz.getName(), modId, name);
                } else {
                    log.error("Did not find {} method with return type String and no parameters.",
                            GET_UNLOCALIZED_NAME_METHOD_NAME);
                }
            }
        } catch (Exception e) {
            log.error("Failed to register all classes marked with @AutoRegister exiting with exception", e);
            throw e;
        }

        registeredCalled = true;
    }

    public void registerRenders() {
        throwIfRegisteredNotCalled();
        loadedClasses.entrySet().forEach(o -> {
            RegistryEntry objectToRegister = o.getValue();
            if (!Item.class.isAssignableFrom(objectToRegister.getClazz())) {
                log.error("Couldn't add render for class {}", objectToRegister.getClazz().getName());
                return;
            }

            registerObjectRender((Item) objectToRegister.getEntry(), objectToRegister.getName(), modId);

            log.info("Registered render for {}.class with mod id {} and name {}",
                    objectToRegister.getClazz().getName(), modId, objectToRegister.getName());
        });
    }

    public void registerRecipes() {
        throwIfRegisteredNotCalled();
        loadedClasses.entrySet().forEach(o -> {
            RegistryEntry registryEntry = o.getValue();
            if (!Craftable.class.isAssignableFrom(registryEntry.getEntry().getClass())) {
                log.info("Skipping registering recipe for class {} since " +
                        "it does not implement the Craftable interface.");
                return;
            }

            Craftable craftable = (Craftable) registryEntry.getEntry();
            registerRecipes(craftable.recipes());
        });
    }

    private void registerRecipes(final List<Recipe> recipes) {
        recipes.forEach(recipe -> {
            Object[] objectRecipe = recipe.getRecipe();
            replaceClassWithInstance(objectRecipe);
            registerRecipe(recipe.getItemStack(), objectRecipe);

            log.info("Finished registering recipe {}", recipe);
        });
    }

    private void replaceClassWithInstance(final Object[] recipe) {
        for (int index = 0; index < recipe.length; index++) {
            Object object = recipe[index];
            if (object instanceof Class) {
                recipe[index] = loadedClasses.get(object).getEntry();
            }
        }
    }

    private void throwIfRegisteredNotCalled() {
        if (!registeredCalled) {
            throw new RuntimeException("You must call ResourceLoader#register " +
                    "before you call other register methods!");
        }
    }

    private static void registerObject(final IForgeRegistryEntry<?> objectToRegister, final String modId, final String name) {
        GameRegistry.register(objectToRegister, new ResourceLocation(modId, name));
    }

    private static void registerObjectRender(final Item itemToRegister, final String name, final String modId) {
        ItemModelMesher itemModelMesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        String pathIn = String.format("%s:%s", modId, name);
        itemModelMesher.register(itemToRegister, 0, new ModelResourceLocation(pathIn, VARIANT_IN));
    }

    private static void registerRecipe(final ItemStack itemStack, final Object[] recipe) {
        GameRegistry.addRecipe(itemStack, recipe);
    }

    @Getter
    @RequiredArgsConstructor
    private static final class RegistryEntry {
        private final IForgeRegistryEntry<?> entry;
        private final String name;
        private final Class clazz;
    }
}
