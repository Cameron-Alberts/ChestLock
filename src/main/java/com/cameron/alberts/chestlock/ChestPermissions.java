package com.cameron.alberts.chestlock;

/**
 * An enum representing the different permissions a user can have on a Chest.
 * A given Permission can modify (add/remove) any user with a lower permission level
 * than them. E.G OWNER can remove a mod/add a mod, but cannot add/remove an OWNER or
 * a ROOT user.
 */
public enum ChestPermissions {
    ROOT(0),
    OWNER(1),
    MOD(2),
    USER(3),
    NONE(4);

    private final int permissionLevel;

    ChestPermissions(final int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }
}