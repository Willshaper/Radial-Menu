package com.cultivate.radialmenu.client;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.cultivate.radialmenu.RadialMenuMod;
import com.cultivate.radialmenu.client.gui.editor.MenuManager;
import com.cultivate.radialmenu.menu.MenuStore;

/**
 * {@code /radialmenu}, a client command:
 *
 * <ul>
 * <li>{@code edit} opens the menu manager.</li>
 * <li>{@code reload} reads menus.json and the icon images again, after editing them outside the game.</li>
 * <li>{@code backup} backs up menus.json now.</li>
 * <li>{@code restore} lists the backups; {@code restore <name>} replaces the menus with one.</li>
 * </ul>
 */
public class RadialMenuCommand extends CommandBase {

    private static final String EDIT = "edit";
    private static final String RELOAD = "reload";
    private static final String BACKUP = "backup";
    private static final String RESTORE = "restore";

    @Override
    public String getCommandName() {
        return RadialMenuMod.MODID;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "radialmenu.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MenuStore store = RadialMenuClient.getMenuStore();

        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        switch (args[0]) {
            case EDIT -> MenuManager.open(null);
            case RELOAD -> {
                RadialMenuClient.reload();
                sender.addChatMessage(
                    new ChatComponentTranslation(
                        "radialmenu.command.reloaded",
                        store.getMenus()
                            .size()));
            }
            case BACKUP -> {
                String name = store.createBackup();

                sender.addChatMessage(
                    name == null ? error("radialmenu.command.backup_failed")
                        : new ChatComponentTranslation("radialmenu.command.backed_up", name));
            }
            case RESTORE -> {
                if (args.length == 1) {
                    listBackups(sender, store);
                } else if (store.restoreBackup(args[1])) {
                    sender.addChatMessage(new ChatComponentTranslation("radialmenu.command.restored", args[1]));
                } else {
                    sender.addChatMessage(error("radialmenu.command.no_backup", args[1]));
                }
            }
            default -> throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    /**
     * Lists the backups, newest first. Clicking one puts its restore command in the chat box.
     */
    private static void listBackups(ICommandSender sender, MenuStore store) {
        List<String> backups = store.getBackups();

        if (backups.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation("radialmenu.command.no_backups"));
            return;
        }
        sender.addChatMessage(new ChatComponentTranslation("radialmenu.command.backups"));
        for (String name : backups) {
            String command = "/" + RadialMenuMod.MODID + " " + RESTORE + " " + name;
            IChatComponent line = new ChatComponentText("  " + name);

            line.setChatStyle(
                new ChatStyle().setColor(EnumChatFormatting.AQUA)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                    .setChatHoverEvent(
                        new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentTranslation("radialmenu.command.restore_hover"))));
            sender.addChatMessage(line);
        }
    }

    private static IChatComponent error(String key, Object... args) {
        IChatComponent message = new ChatComponentTranslation(key, args);

        message.getChatStyle()
            .setColor(EnumChatFormatting.RED);
        return message;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, EDIT, RELOAD, BACKUP, RESTORE);
        }
        if ((args.length == 2) && RESTORE.equals(args[0])) {
            return getListOfStringsFromIterableMatchingLastWord(
                args,
                RadialMenuClient.getMenuStore()
                    .getBackups());
        }
        return null;
    }
}
