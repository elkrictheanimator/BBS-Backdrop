package com.backdrop.client;

import java.io.File;

public class VideoSources
{
    /** The video a pair of settings names: the web address wins over the file in the assets' video folder. */
    public static BackdropVideo source(String url, String file)
    {
        url = url == null ? "" : url.trim();

        if (!url.isEmpty())
        {
            return BackdropVideo.get(url, null);
        }

        String name = file == null ? "" : file.trim();

        if (name.isEmpty())
        {
            return null;
        }

        return BackdropVideo.get(null, new File(Assets.videos(), name));
    }
}
