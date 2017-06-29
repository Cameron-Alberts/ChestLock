package com.cameron.alberts.chestlock;

import lombok.AllArgsConstructor;
import lombok.Value;
import net.minecraft.util.math.BlockPos;

@Value
@AllArgsConstructor
public class ChestPos {
    private final int chestPosX;
    private final int chestPosY;
    private final int chestPosZ;

    public ChestPos(final BlockPos blockPos) {
        chestPosX = blockPos.getX();
        chestPosY = blockPos.getY();
        chestPosZ = blockPos.getZ();
    }

    ChestPos north() {
        return new ChestPos(chestPosX, chestPosY, chestPosZ - 1);
    }

    ChestPos south() {
        return new ChestPos(chestPosX, chestPosY, chestPosZ + 1);
    }

    ChestPos west() {
        return new ChestPos(chestPosX - 1, chestPosY, chestPosZ);
    }

    ChestPos east() {
        return new ChestPos(chestPosX + 1, chestPosY, chestPosZ);
    }
}
