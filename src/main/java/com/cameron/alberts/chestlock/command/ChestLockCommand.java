package com.cameron.alberts.chestlock.command;

import com.cameron.alberts.chestlock.ChestBlock;
import com.cameron.alberts.chestlock.ChestCommandEnum;
import com.cameron.alberts.chestlock.ChestLockManager;
import com.cameron.alberts.chestlock.ChestLockManagerResult;
import com.cameron.alberts.chestlock.ChestPermissions;
import com.cameron.alberts.utils.BlockUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChestLockCommand implements ICommand {
    private static final int USERNAME_INDEX = 1;
    private static final String NAME = "chest";
    private static final String USAGE = "/chest <REGISTER:ADD_OWNER:ADD_MOD:REMOVE> <username>";
    private static final List<String> TAB_COMPLETION_COMMAND = ImmutableList.of(
            "REGISTER",
            "ADD_OWNER",
            "ADD_MOD",
            "ADD_USER",
            "REMOVE"
    );

    private final ChestLockManager manager;

    public ChestLockCommand(final ChestLockManager chestLockManager) {
        this.manager = chestLockManager;
    }

    /**
     * Gets the name of the command.
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Gets the usage string for the command.
     *
     * @param sender The ICommandSender who is requesting usage details
     */
    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    /**
     * Get a list of aliases for this command. <b>Never return null!</b>
     */
    @Override
    public List<String> getAliases() {
        return Lists.newArrayList(NAME);
    }

    /**
     * Callback for when the command is executed
     *
     * @param server The server instance
     * @param sender The sender who executed the command
     * @param args The arguments that were passed
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Invalid command format!"));
            return;
        }

        BlockPos blockPos = BlockUtils.getBlockPosEntityIsLookingAt(sender.getCommandSenderEntity(), 10);
        if (blockPos == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Couldn't find a chest block within 10 blocks of your line of sight!"));
            return;
        }

        World world = sender.getEntityWorld();
        Block block = world.getBlockState(blockPos).getBlock();

        if (!ChestBlock.isChest(block)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "That block isn't a chest!"));
            return;
        }

        ChestBlock chestBlock = new ChestBlock(block.getUnlocalizedName(), blockPos);
        String requestingUsername = sender.getName();
        String userNameForOperation = args[1];

        String[] listOfOnlinePlayers = server.getPlayerList().getOnlinePlayerNames();
        boolean noPlayersMatchPassedName = Arrays.stream(listOfOnlinePlayers).noneMatch(n -> n.equals(userNameForOperation));
        if (noPlayersMatchPassedName) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "That user is not online!"));
        }

        ChestLockManagerResult managerResult;
        switch (ChestCommandEnum.valueOf(args[0])) {
            case REGISTER:
                ChestBlock surroundingChestBlock = ChestBlock.getSurroundingChestBlock(world, blockPos);
                managerResult = manager.register(userNameForOperation, chestBlock, surroundingChestBlock);
                break;
            case ADD_OWNER:
                managerResult = manager.add(requestingUsername, userNameForOperation, chestBlock, ChestPermissions.OWNER);
                break;
            case ADD_MOD:
                managerResult = manager.add(requestingUsername, userNameForOperation, chestBlock, ChestPermissions.MOD);
                break;
            case ADD_USER:
                managerResult = manager.add(requestingUsername, userNameForOperation, chestBlock, ChestPermissions.USER);
                break;
            case REMOVE:
                managerResult = manager.remove(requestingUsername, userNameForOperation, chestBlock);
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Invalid format!"));
                return;
        }
        sender.sendMessage(new TextComponentString(managerResult.getMessage()));
    }

    /**
     * Check if the given ICommandSender has permission to execute this command
     *
     * @param server The server instance
     * @param sender The ICommandSender to check permissions on
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    /**
     * Get a list of options for when the user presses the TAB key
     *
     * @param server The server instance
     * @param sender The ICommandSender to get tab completions for
     * @param args Any arguments that were present when TAB was pressed
     * @param targetPos The block that the player's mouse is over, <tt>null</tt> if the mouse is not over a block
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            String partialCommandName = args[0];
            return TAB_COMPLETION_COMMAND.stream()
                    .filter(s -> s.toLowerCase().startsWith(partialCommandName.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String partialUserName = args[1];
            return Arrays.stream(server.getPlayerList().getOnlinePlayerNames())
                    .filter(s -> s.toLowerCase().startsWith(partialUserName.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Return whether the specified command parameter index is a username parameter.
     *
     * @param args The arguments of the command invocation
     * @param index The index
     */
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == USERNAME_INDEX;
    }

    @Override
    public int compareTo(ICommand command) {
        return command.getName().compareTo(getName());
    }
}
