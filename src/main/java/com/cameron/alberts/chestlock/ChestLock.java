package com.cameron.alberts.chestlock;

import lombok.Value;

@Value
public class ChestLock {
    private String userName;
    private ChestPos chestPos;
    private ChestCommandEnum command;
}
