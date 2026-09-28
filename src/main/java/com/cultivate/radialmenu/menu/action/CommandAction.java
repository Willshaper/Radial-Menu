package com.cultivate.radialmenu.menu.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraftforge.client.ClientCommandHandler;

/**
 * Runs a command, the same way typing it into chat would: client-side commands first, then the server.
 */
public class CommandAction implements SectionAction {

    private final String command;

    public CommandAction(String command) {
        this.command = command.startsWith("/") ? command : "/" + command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public ActionType getType() {
        return ActionType.COMMAND;
    }

    @Override
    public void run() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if (player == null) {
            return;
        }
        // Zero means no client-side command claimed it, which is how GuiChat decides to send it to the server.
        if (ClientCommandHandler.instance.executeCommand(player, command) == 0) {
            player.sendChatMessage(command);
        }
    }

    @Override
    public String describe() {
        return command;
    }
}
