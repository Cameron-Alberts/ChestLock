package com.cameron.alberts.utils;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class BlockUtils {
    private static final float PARTIAL_TICKS = 1.0f;

    @Nullable
    public static BlockPos getBlockPosEntityIsLookingAt(@Nullable final Entity entity, final int maxDistance) {
        // Guard against NullPointerException
        if (entity == null) {
            return null;
        }

        World world = entity.getEntityWorld();
        Vec3d vec3d = entity.getPositionEyes(PARTIAL_TICKS);
        Vec3d vec3d1 = entity.getLook(PARTIAL_TICKS);
        Vec3d vec3d2 = vec3d.addVector(vec3d1.xCoord * maxDistance, vec3d1.yCoord * maxDistance, vec3d1.zCoord * maxDistance);

        RayTraceResult rayTraceResult = world.rayTraceBlocks(vec3d, vec3d2);

        // Ray trace can be null, return null if so.
        if (rayTraceResult == null) {
            return null;
        }

        return rayTraceResult.getBlockPos();
    }

    public static List<BlockPos> getSurroundingBlockPos(final BlockPos blockPos) {
        return ImmutableList.of(
                blockPos.north(),
                blockPos.south(),
                blockPos.west(),
                blockPos.east()
        );
    }
}
