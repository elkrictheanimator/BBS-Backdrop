package com.backdrop.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

/** BBS's assets folder, where the videos live (assets/video). */
public class Assets
{
    public static File videos()
    {
        try
        {
            Object folder = Class.forName("mchorse.bbs_mod.BBSMod").getMethod("getAssetsFolder").invoke(null);

            if (folder instanceof File)
            {
                return new File((File) folder, "video");
            }
        }
        catch (Throwable ignored)
        {}

        return FabricLoader.getInstance().getGameDir().resolve("config/bbs/assets/video").toFile();
    }
}
