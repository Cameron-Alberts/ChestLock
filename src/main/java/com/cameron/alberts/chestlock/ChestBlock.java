package com.cameron.alberts.chestlock;

import com.cameron.alberts.utils.BlockUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Value
@AllArgsConstructor
public class ChestBlock {
    private static final Set<String> POSSIBLE_DOUBLE_CHESTS_NAMES = ImmutableSet.of(
        Blocks.CHEST.getUnlocalizedName()
    );

    private static final Set<String> CHEST_NAMES = ImmutableSet.of(
            Blocks.CHEST.getUnlocalizedName(),
            Blocks.ENDER_CHEST.getUnlocalizedName()
    );

    private final String unlocalizedName;
    private final ChestPos chestPos;

    public ChestBlock(final String unlocalizedName, final BlockPos blockPos) {
       this.unlocalizedName = unlocalizedName;
       this.chestPos = new ChestPos(blockPos);
    }

    public static boolean isDoubleChestBlockType(final Block block) {
        return POSSIBLE_DOUBLE_CHESTS_NAMES.contains(block.getUnlocalizedName());
    }

    public static boolean isChest(final Block block) {
        return CHEST_NAMES.contains(block.getUnlocalizedName());
    }

    public static List<ChestBlock> getSurroundingChestBlocks(final ChestBlock chestBlock) {
        final String unlocalizedName = chestBlock.getUnlocalizedName();
        final ChestPos chestPos = chestBlock.getChestPos();
        return ImmutableList.of(
                new ChestBlock(unlocalizedName, chestPos.north()),
                new ChestBlock(unlocalizedName, chestPos.south()),
                new ChestBlock(unlocalizedName, chestPos.west()),
                new ChestBlock(unlocalizedName, chestPos.east())
        );
    }

    @Nullable
    public static ChestBlock getSurroundingChestBlock(final World world, final BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        String blockNameToFind = block.getUnlocalizedName();

        for (BlockPos bp : BlockUtils.getSurroundingBlockPos(blockPos)) {
            Block foundBlock = world.getBlockState(bp).getBlock();

            if (isDoubleChestBlockType(foundBlock) && foundBlock.getUnlocalizedName().equals(blockNameToFind)) {
                return new ChestBlock(blockNameToFind, bp);
            }
        }

        return null;
    }
}
