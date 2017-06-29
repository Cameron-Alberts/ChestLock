package com.cameron.alberts.chestlock;

import net.minecraft.util.text.TextFormatting;

public enum ChestLockManagerResult {
    PERMISSION_DENIED(TextFormatting.RED + "You do not have high enough permission levels to run this command!"),
    CHEST_ALREADY_REGISTERED(TextFormatting.RED + "This chest has already been claimed!"),
    CHEST_IS_UNREGISTERED(TextFormatting.RED + "That chest is not registered to anyone!"),
    USER_IS_UNREGISTERED(TextFormatting.RED + "The user isn't registered with this chest!"),
    CANT_MODIFY_YOUR_OWN_PERMISSIONS(TextFormatting.RED + "You cannot modify your own permissions!"),
    SURROUNDING_CHEST_REGISTERED(TextFormatting.RED + "The surrounding chest is already registered!"),
    USER_ALREADY_HAS_THOSE_PERMISSIONS(TextFormatting.YELLOW + "That user already has those permissions!"),
    SUCCESSFULLY_REMOVED_USER(TextFormatting.GREEN + "Successfully removed user from chest!"),
    SUCCESSFULLY_ADDED_USER(TextFormatting.GREEN + "Successfully added user to chest!"),
    SUCCESSFULLY_REGISTERED_CHEST(TextFormatting.GREEN + "Successfully registered chest!");

    private final String message;

    ChestLockManagerResult(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
