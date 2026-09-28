package com.cultivate.radialmenu.menu.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

/**
 * Sends a plain chat message.
 */
public class ChatAction implements SectionAction {

    private final String message;

    public ChatAction(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public ActionType getType() {
        return ActionType.CHAT;
    }

    @Override
    public void run() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        if ((player != null) && !message.isEmpty()) {
            player.sendChatMessage(message);
        }
    }

    @Override
    public String describe() {
        return message;
    }
}
