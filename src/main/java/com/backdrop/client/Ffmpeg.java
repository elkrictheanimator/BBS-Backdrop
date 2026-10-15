package com.backdrop.client;

import com.backdrop.BackdropConfig;

/** Where ffmpeg is: this addon's own setting, else the one set in BBS (Settings > Video), else whatever is on the PATH. */
public class Ffmpeg
{
    public static String path()
    {
        String own = BackdropConfig.get().ffmpeg;

        if (own != null && !own.trim().isEmpty())
        {
            return own.trim();
        }

        try
        {
            Object path = Class.forName("mchorse.bbs_mod.utils.FFMpegUtils").getMethod("getFFMPEG").invoke(null);

            if (path != null && !path.toString().isEmpty())
            {
                return path.toString();
            }
        }
        catch (Throwable ignored)
        {}

        return "ffmpeg";
    }
}
