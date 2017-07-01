package com.cameron.alberts.chestlock;

import com.cameron.alberts.metrics.TimerMetric;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Value;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChestLockManager extends WorldSavedData {
    private static final String CHEST_LOCK_MANAGER_SAVE_KEY = ChestLockMod.MOD_ID + "_ChestLockManager";
    private static final String CHEST_PERMISSIONS_SAVE_KEY = CHEST_LOCK_MANAGER_SAVE_KEY + "_ChestPermissions";
    private static final String CHEST_OWNERS_SAVE_KEY = CHEST_LOCK_MANAGER_SAVE_KEY + "_ChestOwners";
    private static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().create();

    private final Object REGISTER_LOCK = new Object();
    private Map<ChestBlock, Map<String, ChestPermissions>> chestPermissions;
    private Map<ChestBlock, String> chestOwners;

    /**
     * To prevent possible race conditions there is a synchronized block. This method is intended to register
     * a ChestBlock to a given user with {@link ChestPermissions#ROOT} permissions. It will also handle double
     * chest blocks and correctly registering them.
     * @param userName {@link EntityPlayer#getName()} to try and register to this block, can be overrode if
     * this block happens to be connected to a chest that has already been registered.
     * @param chestBlock {@link ChestBlock} to register to attempt to register to this user.
     * @param surroundingChestBlock if not null represents a ChestBlock that is the same type as {@code chestBlock},
     * and will be handled for edge cases.
     * @return a {@link ChestLockManagerResult} representing whether this was successful or had an error.
     */
    public ChestLockManagerResult register(final String userName, final ChestBlock chestBlock, @Nullable final ChestBlock surroundingChestBlock) {
        if (chestPermissions.containsKey(chestBlock)) {
            return ChestLockManagerResult.CHEST_ALREADY_REGISTERED;
        }

        // Prevent possible race conditions
        synchronized (REGISTER_LOCK) {
            // There was a ChestBlock with the same name (type) as the passed in chestBlock
            if (surroundingChestBlock != null) {
                // It is managed by this class
                if(contains(surroundingChestBlock)) {
                    // Let the ChestLockEvents class and the user know they do not have permissions
                    // to the surrounding chest
                    if (!hasPermissions(userName, surroundingChestBlock)) {
                        return ChestLockManagerResult.SURROUNDING_CHEST_REGISTERED;
                    }

                    // Thus far we know the surroundingChestBlock is managed by this class
                    // and the user has permissions to it, meaning the root user can either
                    // be the person laying it or someone else. Look up the root user and
                    // and set the chestBlock's permissions to the same reference as
                    // surroundingChestBlocks. Also set the root user for chestBlock to be the
                    // same as the root user for surroundingChestBlock. This will take care
                    // of both cases.
                    String rootUserName = chestOwners.get(surroundingChestBlock);
                    chestPermissions.put(chestBlock, chestPermissions.get(surroundingChestBlock));
                    chestOwners.put(chestBlock, rootUserName);

                    // Must return to prevent registering userName as an additional root
                    // on this chestBlock
                    setDirty(true);
                    return ChestLockManagerResult.SUCCESSFULLY_REGISTERED_CHEST;
                }

                // If it has gotten this far that means no one owns this block so register it
                // to userName
                register(userName, surroundingChestBlock);
            }

            // Either there was no surrounding block or the surrounding block wasn't registered
            // to this class
            register(userName, chestBlock);
        }

        setDirty(true);
        return ChestLockManagerResult.SUCCESSFULLY_REGISTERED_CHEST;
    }

    /**
     * A user can use the {@link com.cameron.alberts.chestlock.command.ChestLockCommand} to add who has
     * access to their chests and with what permissions.
     * @param requestingUserName {@link EntityPlayer#getName()} who called the ChestLockCommand.
     * @param userName argument in ChestLockCommand representing another players {@link EntityPlayer#getName()}
     * for which to add access for.
     * @param chestBlock the chest block in line of sight of the {@code requestingUserName}.
     * @param permissionToGive see {@link ChestPermissions} to understand the different permissions.
     * @return a {@link ChestLockManagerResult} representing whether this was successful or had an error.
     */
    public ChestLockManagerResult add(final String requestingUserName,
                                      final String userName,
                                      final ChestBlock chestBlock,
                                      final ChestPermissions permissionToGive) {
        return updatePermission(requestingUserName, userName, chestBlock,
                permissionToGive, Operation.ADD);
    }

    /**
     * A user can use the {@link com.cameron.alberts.chestlock.command.ChestLockCommand} to remove
     * access to their chests.
     * @param requestingUserName {@link EntityPlayer#getName()} who called the ChestLockCommand.
     * @param userName argument in ChestLockCommand representing another players {@link EntityPlayer#getName()}
     * for which to remove access for.
     * @param chestBlock the chest block in line of sight of the {@code requestingUserName}.
     * @return a {@link ChestLockManagerResult} representing whether this was successful or had an error.
     */
    public ChestLockManagerResult remove(final String requestingUserName,
                                         final String userName,
                                         final ChestBlock chestBlock) {
        ChestPermissions userChestPermission = getPermission(userName, chestBlock);
        return updatePermission(requestingUserName, userName, chestBlock,
                userChestPermission, Operation.REMOVE);
    }

    public boolean canOpen(final String userName, final ChestBlock chestBlock) {
        return !contains(chestBlock) || hasPermissions(userName, chestBlock);
    }

    /**
     * Checks if the block is currently registered (locked).
     * @param chestBlock the {@link ChestBlock} to check.
     * @return true if it is registered (locked), otherwise false.
     */
    public boolean contains(final ChestBlock chestBlock) {
        return chestPermissions.containsKey(chestBlock);
    }

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        try (TimerMetric metric = TimerMetric.create("readFromNBT")) {
            List<ChestBlockWithPermissionsMap> chestPermissionsEntrySet =
                    GSON.fromJson(compound.getString(CHEST_PERMISSIONS_SAVE_KEY),
                            new TypeToken<List<ChestBlockWithPermissionsMap>>() {}.getType());
            Map<ChestBlock, Map<String, ChestPermissions>> localChestPermissions =
                    new ConcurrentHashMap<>(chestPermissionsEntrySet.size());

            // chestPermissions is stored in NBT as a json representing its entry set, this is because this class
            // takes advantage of references to Map<String, ChestPermissions>. If there is a double chest each
            // ChestBlock in the double chest points to the same Map<String, ChestPermissions> reference, so this
            // guarantees that they point to the same reference.
            for (ChestBlockWithPermissionsMap entry : chestPermissionsEntrySet) {
                List<ChestBlock> possibleDoubleChestBlocks = ChestBlock.getSurroundingChestBlocks(entry.getChestBlock());
                Optional<ChestBlock> matchingBlockInMap = possibleDoubleChestBlocks
                        .stream()
                        .filter(localChestPermissions::containsKey)
                        .findFirst();

                if (matchingBlockInMap.isPresent()) {
                    localChestPermissions.put(entry.getChestBlock(), localChestPermissions.get(matchingBlockInMap.get()));
                } else {
                    localChestPermissions.put(entry.getChestBlock(), new ConcurrentHashMap<>(entry.getPermissionsMap()));
                }
            }

            chestPermissions = localChestPermissions;
            chestOwners = GSON.fromJson(compound.getString(CHEST_OWNERS_SAVE_KEY),
                    new TypeToken<ConcurrentHashMap<ChestBlock, String>>() {}.getType());
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        try (TimerMetric metric = TimerMetric.create("writeToNBT")) {
            List<ChestBlockWithPermissionsMap> chestBlockWithPermissionsMaps =
                    chestPermissions
                            .entrySet()
                            .stream()
                            .map(e -> new ChestBlockWithPermissionsMap(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

            compound.setString(CHEST_PERMISSIONS_SAVE_KEY, GSON.toJson(chestBlockWithPermissionsMaps));
            compound.setString(CHEST_OWNERS_SAVE_KEY, GSON.toJson(chestOwners));

            return compound;
        }
    }

    /**
     * If a user has permissions to open a given {@link ChestBlock}.
     * @param userName {@link EntityPlayer#getName()} who attempted to open the ChestBlock.
     * @param chestBlock the {@link ChestBlock} to check
     * @return true if the user has any permission except {@link ChestPermissions#NONE}.
     */
    private boolean hasPermissions(final String userName, final ChestBlock chestBlock) {
        return !getPermission(userName, chestBlock).equals(ChestPermissions.NONE);
    }

    private ChestLockManagerResult updatePermission(final String requestingUserName,
                                                    final String userName,
                                                    final ChestBlock chestBlock,
                                                    final ChestPermissions permissionToSet,
                                                    final Operation operation) {
        Map<String, ChestPermissions> permissionsMap = chestPermissions.get(chestBlock);

        // If it isn't in the chestPermissions map it hasn't been registered.
        if (permissionsMap == null) {
            return ChestLockManagerResult.CHEST_IS_UNREGISTERED;
        }

        // If this person is not at a higher permission level than the permission they're trying to modify.
        if (!hasPermissionToModify(requestingUserName, chestBlock, permissionToSet)) {
            return ChestLockManagerResult.PERMISSION_DENIED;
        }

        // For consistency lets not let users edit their own permissions.
        if (requestingUserName.equals(userName)) {
            return ChestLockManagerResult.CANT_MODIFY_YOUR_OWN_PERMISSIONS;
        }

        // Instead of getPermission returning a null, which causes an edge case, it returns
        // ChestPermissions.NONE which means the user isn't associated to this chest at all.
        // The checks are purposely done in this order, so it makes sense from all use cases.
        // If you do not have permission to access this chest in the first place then you
        // shouldn't be able to know if a user is registered to it or not.
        if (permissionToSet.equals(ChestPermissions.NONE)) {
            return ChestLockManagerResult.USER_IS_UNREGISTERED;
        }

        switch(operation) {
            case ADD:
                // If the user already has that permission lets not return a success message
                if (getPermission(userName, chestBlock).equals(permissionToSet)) {
                    return ChestLockManagerResult.USER_ALREADY_HAS_THOSE_PERMISSIONS;
                }

                permissionsMap.put(userName, permissionToSet);

                return ChestLockManagerResult.SUCCESSFULLY_ADDED_USER;
            case REMOVE:
                permissionsMap.remove(userName);

                return ChestLockManagerResult.SUCCESSFULLY_REMOVED_USER;
            default:
                throw new RuntimeException("Invalid operation!");
        }
    }

    private void register(final String userName, final ChestBlock blockToRegister) {
        chestPermissions.computeIfAbsent(blockToRegister, c -> Maps.newConcurrentMap()).put(userName, ChestPermissions.ROOT);
        chestOwners.put(blockToRegister, userName);
    }

    private boolean hasPermissionToModify(final String userName, final ChestBlock chestBlock, ChestPermissions permissionToModify) {
        ChestPermissions requestingUserPermission = getPermission(userName, chestBlock);
        return requestingUserPermission != null && requestingUserPermission.getPermissionLevel() < permissionToModify.getPermissionLevel();
    }

    private ChestPermissions getPermission(final String userName, final ChestBlock chestBlock) {
        Map<String, ChestPermissions> chestPermissionsMap = chestPermissions.get(chestBlock);

        if (chestPermissionsMap == null) {
            return ChestPermissions.NONE;
        }

        ChestPermissions chestPermissions = chestPermissionsMap.get(userName);

        if (chestPermissions == null) {
            return ChestPermissions.NONE;
        }

        return chestPermissions;
    }

    static ChestLockManager getChestLockManager(final World world) {
        MapStorage storage = world.getMapStorage();
        ChestLockManager chestLockManager = (ChestLockManager) storage.getOrLoadData(ChestLockManager.class, CHEST_LOCK_MANAGER_SAVE_KEY);

        if (chestLockManager == null) {
            chestLockManager = new ChestLockManager();
            storage.setData(CHEST_LOCK_MANAGER_SAVE_KEY, chestLockManager);
        }

        return chestLockManager;
    }

    public ChestLockManager(final String name) {
        super(name);
    }

    private ChestLockManager() {
        super(CHEST_LOCK_MANAGER_SAVE_KEY);
        chestPermissions = Maps.newConcurrentMap();
        chestOwners = Maps.newConcurrentMap();
    }

    @Value
    private static final class ChestBlockWithPermissionsMap {
        private final ChestBlock chestBlock;
        private final Map<String, ChestPermissions> permissionsMap;
    }

    private enum Operation {
        ADD,
        REMOVE
    }
}
