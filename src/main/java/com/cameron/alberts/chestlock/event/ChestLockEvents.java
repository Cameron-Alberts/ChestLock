package com.cameron.alberts.chestlock.event;

import com.cameron.alberts.chestlock.ChestBlock;
import com.cameron.alberts.chestlock.ChestLockManager;
import com.cameron.alberts.chestlock.ChestLockManagerResult;
import com.cameron.alberts.metrics.TimerMetric;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ChestLockEvents {
    private static final Set<String> CHESTS = ImmutableSet.of(
            Blocks.CHEST.getUnlocalizedName(),
            Blocks.ENDER_CHEST.getUnlocalizedName()
    );

    private final ChestLockManager manager;

    public ChestLockEvents(final ChestLockManager chestLockManager) {
        this.manager = chestLockManager;
    }

    @SubscribeEvent
    public void handleExplosion(final ExplosionEvent explosionEvent) {
        World world = explosionEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        try (TimerMetric timerMetric = TimerMetric.create("handleExplosion")) {
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
                if (manager.contains(chestBlock)) {
                    blockPosIterator.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void handlePlayerInteractLeftClick(final PlayerInteractEvent.LeftClickBlock leftClickEvent) {
        World world = leftClickEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        try (TimerMetric metric = TimerMetric.create("handlePlayerInteractLeftClick")) {
            BlockPos blockPos = leftClickEvent.getPos();
            Block block = world.getBlockState(blockPos).getBlock();

            if (!ChestBlock.isChest(block)) {
                return;
            }

            ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);
            if (manager.contains(chestBlock)) {
                leftClickEvent.setCanceled(true);
                leftClickEvent.getEntity().sendMessage(new TextComponentString(TextFormatting.RED + "This chest is protected!"));
            }
        }
    }

    @SubscribeEvent
    public void handlePlayerInteractRightClick(final PlayerInteractEvent.RightClickBlock rightClickEvent) {
        World world = rightClickEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        try (TimerMetric metric = TimerMetric.create("handlePlayerInteractRightClick")) {
            BlockPos blockPos = rightClickEvent.getPos();
            Block block = world.getBlockState(blockPos).getBlock();

            if (!ChestBlock.isChest(block)) {
                return;
            }

            ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);
            if (!manager.canOpen(rightClickEvent.getEntity().getName(), chestBlock)) {
                rightClickEvent.setCanceled(true);
                rightClickEvent.getEntity().sendMessage(new TextComponentString(TextFormatting.RED + "You do not have access to this chest!"));
            }
        }
    }

    @SubscribeEvent
    public void handleBlockPlacedEvent(final BlockEvent.PlaceEvent placeEvent) {
        World world = placeEvent.getWorld();

        if (world.isRemote) {
            return;
        }

        try (TimerMetric metric = TimerMetric.create("handleBlockPlacedEvent")) {
            Block placedBlock = placeEvent.getPlacedBlock().getBlock();
            BlockPos blockPos = placeEvent.getPos();

            if (!ChestBlock.isChest(placedBlock)) {
                return;
            }

            EntityPlayer player = placeEvent.getPlayer();
            ChestBlock chestBlock = new ChestBlock(placedBlock.getUnlocalizedName(), blockPos);
            ChestBlock surroundingChestBlock = ChestBlock.getSurroundingChestBlock(world, blockPos);

            ChestLockManagerResult chestLockManagerResult = manager.register(player.getName(), chestBlock, surroundingChestBlock);
            if (chestLockManagerResult.equals(ChestLockManagerResult.SURROUNDING_CHEST_REGISTERED)) {
                placeEvent.setCanceled(true);
            }

            placeEvent.getPlayer().sendMessage(new TextComponentString(chestLockManagerResult.getMessage()));
        }
    }
}
