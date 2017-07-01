package com.cameron.alberts.chestlock;

import com.cameron.alberts.chestlock.command.ChestLockCommand;
import com.cameron.alberts.chestlock.event.ChestLockEvents;
import com.cameron.alberts.chestlock.proxy.CommonProxy;
import com.cameron.alberts.loader.ResourceLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

@Mod(modid = ChestLockMod.MOD_ID, name = ChestLockMod.MOD_NAME, version = ChestLockMod.VERSION)
public class ChestLockMod {
    private static final String CLIENT_PROXY_CLASS_PATH = "com.cameron.alberts.chestlock.proxy.ClientProxy";
    private static final String COMMON_PROXY_CLASS_PATH = "com.cameron.alberts.chestlock.proxy.CommonProxy";

    @Mod.Instance
    private static ChestLockMod mod;

    @SidedProxy(
            modId = ChestLockMod.MOD_ID,
            clientSide = CLIENT_PROXY_CLASS_PATH,
            serverSide = COMMON_PROXY_CLASS_PATH
    )
    private static CommonProxy proxy;

    static final String MOD_NAME = "Chest Lock";
    static final String VERSION = "1.0";

    public static final String MOD_ID = "chest_lock";

    public static ResourceLoader resourceLoader = new ResourceLoader("com.cameron.alberts.chestlock", MOD_ID);
    public static ChestLockManager chestLockManager;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        resourceLoader.register();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        resourceLoader.registerRecipes();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        chestLockManager = ChestLockManager.getChestLockManager(event.getServer().getEntityWorld());
        MinecraftForge.EVENT_BUS.register(new ChestLockEvents(chestLockManager));
        event.registerServerCommand(new ChestLockCommand(chestLockManager));
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // ChestLockManager periodically sets its dirty bit, make sure it was
        // set before stopping the server.
        if (chestLockManager != null) {
            chestLockManager.setDirty(true);
        }
    }
}
