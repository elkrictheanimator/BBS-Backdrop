package com.backdrop;

import net.minecraft.client.MinecraftClient;

/** English and Vietnamese, by the language BBS is set to (else the game's). More languages are one more argument. */
public class Lang
{
    public static boolean vietnamese()
    {
        try
        {
            Class<?> settings = Class.forName("mchorse.bbs_mod.BBSSettings");
            Object value = settings.getField("language").get(null);
            Object language = value.getClass().getMethod("get").invoke(value);

            if (language != null && !language.toString().isEmpty())
            {
                return language.toString().toLowerCase().startsWith("vi");
            }
        }
        catch (Throwable ignored)
        {}

        try
        {
            return MinecraftClient.getInstance().options.language.toLowerCase().startsWith("vi");
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    public static String tr(String en, String vi)
    {
        return vietnamese() ? vi : en;
    }
}
