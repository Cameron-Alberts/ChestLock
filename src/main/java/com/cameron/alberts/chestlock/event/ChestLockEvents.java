package com.cameron.alberts.chestlock.event;

import com.cameron.alberts.chestlock.ChestBlock;
import com.cameron.alberts.chestlock.ChestLockManager;
import com.cameron.alberts.chestlock.ChestLockManagerResult;
import com.cameron.alberts.chestlock.ChestLockMod;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = ChestLockMod.MOD_ID)
public class ChestLockEvents {
    private static final ChestLockManager MANAGER = ChestLockManager.singleton();
    private static final Set<String> CHESTS = ImmutableSet.of(
            Blocks.CHEST.getUnlocalizedName(),
            Blocks.ENDER_CHEST.getUnlocalizedName()
    );

    @SubscribeEvent
    public static void handleExplosion(final ExplosionEvent explosionEvent) {
        Stopwatch stopWatch = Stopwatch.createStarted();

        World world = explosionEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        List<BlockPos> blockPosList = explosionEvent.getExplosion().getAffectedBlockPositions();

        Iterator<BlockPos> blockPosIterator = blockPosList.iterator();
        while (blockPosIterator.hasNext()) { // Iterate over blocks in the explosion
            BlockPos blockPos = blockPosIterator.next();
            Block block = world.getBlockState(blockPos).getBlock();

            if (!ChestBlock.isChest(block)) {
                continue;
            }

            ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);

            // If the current block is owned by someone remove it from being exploded
            if (MANAGER.contains(chestBlock)) {
                blockPosIterator.remove();
            }
        }

        System.out.println(stopWatch.elapsed(TimeUnit.MICROSECONDS));
    }

    @SubscribeEvent
    public static void handlePlayerInteractLeftClick(final PlayerInteractEvent.LeftClickBlock leftClickEvent) {
        Stopwatch start = Stopwatch.createStarted();

        World world = leftClickEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        BlockPos blockPos = leftClickEvent.getPos();
        Block block = world.getBlockState(blockPos).getBlock();

        if (!ChestBlock.isChest(block)) {
            return;
        }

        ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);
        if (MANAGER.contains(chestBlock)) {
            leftClickEvent.setCanceled(true);
            leftClickEvent.getEntity().sendMessage(new TextComponentString(TextFormatting.RED + "This chest is protected!"));
        }

        System.out.println(start.elapsed(TimeUnit.MICROSECONDS));
    }

    @SubscribeEvent
    public static void handlePlayerInteractRightClick(final PlayerInteractEvent.RightClickBlock rightClickEvent) {
        Stopwatch start = Stopwatch.createStarted();
        World world = rightClickEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        BlockPos blockPos = rightClickEvent.getPos();
        Block block = world.getBlockState(blockPos).getBlock();

        if (!ChestBlock.isChest(block)) {
            return;
        }

        ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);
        if (!MANAGER.canOpen(rightClickEvent.getEntity().getName(), chestBlock)) {
            rightClickEvent.setCanceled(true);
            rightClickEvent.getEntity().sendMessage(new TextComponentString(TextFormatting.RED + "You do not have access to this chest!"));
        }

        System.out.println(start.elapsed(TimeUnit.MICROSECONDS));
    }


    @SubscribeEvent
    public static void handleBlockPlacedEvent(final BlockEvent.PlaceEvent placeEvent) {
        Stopwatch start = Stopwatch.createStarted();
        World world = placeEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        Block placedBlock = placeEvent.getPlacedBlock().getBlock();
        BlockPos blockPos = placeEvent.getPos();

        if (!ChestBlock.isChest(placedBlock)) {
            return;
        }

        EntityPlayer player = placeEvent.getPlayer();
        ChestBlock chestBlock = new ChestBlock(placedBlock.getUnlocalizedName(), blockPos);
        ChestBlock surroundingChestBlock = ChestBlock.getSurroundingChestBlock(world, blockPos);

        ChestLockManagerResult chestLockManagerResult = MANAGER.register(player.getName(), chestBlock, surroundingChestBlock);
        if (chestLockManagerResult.equals(ChestLockManagerResult.SURROUNDING_CHEST_REGISTERED)) {
            placeEvent.setCanceled(true);
        }

        placeEvent.getPlayer().sendMessage(new TextComponentString(chestLockManagerResult.getMessage()));

        System.out.println(start.elapsed(TimeUnit.MICROSECONDS));
    }
}
