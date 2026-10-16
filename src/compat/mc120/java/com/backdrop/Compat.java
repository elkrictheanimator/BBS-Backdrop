package com.backdrop;

import net.minecraft.util.Identifier;

/** What differs between the Minecraft versions this addon is built for; one copy per src/compat/<version>. */
public class Compat
{
    public static Identifier id(String namespace, String path)
    {
        return new Identifier(namespace, path);
    }
}
