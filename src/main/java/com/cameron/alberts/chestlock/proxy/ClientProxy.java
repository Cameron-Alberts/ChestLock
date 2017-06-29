package com.cameron.alberts.chestlock.proxy;

import com.cameron.alberts.chestlock.ChestLockMod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {}

    @Override
    public void init(FMLInitializationEvent event) {
        ChestLockMod.RESOURCE_LOADER.registerRenders();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {}
}
