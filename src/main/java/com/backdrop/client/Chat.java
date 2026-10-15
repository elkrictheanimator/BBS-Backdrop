package com.backdrop.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** A line in the chat, from any thread. */
public class Chat
{
    public static void say(String message)
    {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() ->
        {
            if (client.inGameHud != null)
            {
                client.inGameHud.getChatHud().addMessage(Text.literal("[Backdrop] " + message));
            }
        });
    }
}
