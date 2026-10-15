package com.backdrop.client;

import com.backdrop.BackdropConfig;
import com.backdrop.Lang;
import com.backdrop.spotify.Spotify;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.util.Arrays;

/** {@code /backdrop ...}: every setting, from chat — no dependence on any BBS settings screen. */
public class BackdropCommands
{
    private interface Edit
    {
        void apply(BackdropConfig.Source source);
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("backdrop");

        root.executes(c -> help(c));
        root.then(ClientCommandManager.literal("help").executes(c -> help(c)));
        root.then(ClientCommandManager.literal("videos").executes(c -> videos(c)));
        root.then(source("banner"));
        root.then(source("wallpaper"));

        root.then(ClientCommandManager.literal("darkness")
            .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0, 1)).executes(c ->
            {
                BackdropConfig.get().darkness = FloatArgumentType.getFloat(c, "value");

                return done(c);
            })));
        root.then(ClientCommandManager.literal("panels")
            .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0, 1)).executes(c ->
            {
                BackdropConfig.get().panelOpacity = FloatArgumentType.getFloat(c, "value");

                return done(c);
            })));
        root.then(ClientCommandManager.literal("ffmpeg")
            .then(ClientCommandManager.argument("path", StringArgumentType.greedyString()).executes(c ->
            {
                BackdropConfig.get().ffmpeg = StringArgumentType.getString(c, "path");

                return done(c);
            })));

        root.then(spotify());

        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> source(String name)
    {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal(name);

        node.then(ClientCommandManager.literal("video")
            .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                .suggests((context, builder) ->
                {
                    for (String file : videoNames())
                    {
                        if (file.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                        {
                            builder.suggest(file);
                        }
                    }

                    return builder.buildFuture();
                })
                .executes(c -> edit(c, name, s ->
                {
                    s.video = StringArgumentType.getString(c, "name").trim();
                    s.url = "";
                    s.enabled = true;
                }))));
        node.then(ClientCommandManager.literal("url")
            .then(ClientCommandManager.argument("link", StringArgumentType.greedyString())
                .executes(c -> edit(c, name, s ->
                {
                    s.url = StringArgumentType.getString(c, "link").trim();
                    s.enabled = true;
                }))));
        node.then(ClientCommandManager.literal("on").executes(c -> edit(c, name, s -> s.enabled = true)));
        node.then(ClientCommandManager.literal("off").executes(c -> edit(c, name, s -> s.enabled = false)));
        node.then(ClientCommandManager.literal("clear").executes(c -> edit(c, name, s ->
        {
            s.video = "";
            s.url = "";
        })));
        node.then(ClientCommandManager.literal("volume")
            .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0, 1))
                .executes(c -> edit(c, name, s -> s.volume = FloatArgumentType.getFloat(c, "value")))));
        node.then(ClientCommandManager.literal("sync")
            .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(-3000, 3000))
                .executes(c -> edit(c, name, s -> s.sync = IntegerArgumentType.getInteger(c, "ms")))));

        return node;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> spotify()
    {
        LiteralArgumentBuilder<FabricClientCommandSource> node = ClientCommandManager.literal("spotify");

        node.then(ClientCommandManager.literal("id")
            .then(ClientCommandManager.argument("clientId", StringArgumentType.word()).executes(c ->
            {
                BackdropConfig.get().spotify.clientId = StringArgumentType.getString(c, "clientId");

                return done(c);
            })));
        node.then(ClientCommandManager.literal("login").executes(c ->
        {
            Spotify.login();

            return 1;
        }));
        node.then(ClientCommandManager.literal("logout").executes(c ->
        {
            Spotify.logout();

            return done(c);
        }));
        node.then(ClientCommandManager.literal("play").executes(c -> control(Spotify::playPause)));
        node.then(ClientCommandManager.literal("pause").executes(c -> control(Spotify::playPause)));
        node.then(ClientCommandManager.literal("next").executes(c -> control(Spotify::next)));
        node.then(ClientCommandManager.literal("prev").executes(c -> control(Spotify::previous)));
        node.then(ClientCommandManager.literal("volume")
            .then(ClientCommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
                .executes(c -> control(() -> Spotify.setVolume(IntegerArgumentType.getInteger(c, "percent"))))));
        node.then(ClientCommandManager.literal("card")
            .then(ClientCommandManager.argument("show", BoolArgumentType.bool()).executes(c ->
            {
                BackdropConfig.get().spotify.card = BoolArgumentType.getBool(c, "show");

                return done(c);
            })));

        return node;
    }

    private static int control(Runnable action)
    {
        if (!Spotify.signedIn())
        {
            Chat.say(Lang.tr("Not signed in: /backdrop spotify login", "Chưa đăng nhập: /backdrop spotify login"));

            return 0;
        }

        action.run();

        return 1;
    }

    private static int edit(CommandContext<FabricClientCommandSource> context, String which, Edit edit)
    {
        BackdropConfig config = BackdropConfig.get();

        edit.apply(which.equals("banner") ? config.banner : config.wallpaper);

        return done(context);
    }

    private static int done(CommandContext<FabricClientCommandSource> context)
    {
        BackdropConfig.get().save();
        context.getSource().sendFeedback(Text.literal("[Backdrop] " + Lang.tr("Saved.", "Đã lưu.")));

        return 1;
    }

    private static String[] videoNames()
    {
        File[] files = Assets.videos().listFiles();

        if (files == null)
        {
            return new String[0];
        }

        return Arrays.stream(files).filter(File::isFile).map(File::getName).sorted().toArray(String[]::new);
    }

    private static int videos(CommandContext<FabricClientCommandSource> context)
    {
        String[] names = videoNames();

        context.getSource().sendFeedback(Text.literal("[Backdrop] " + Assets.videos() + ": " + (names.length == 0 ? Lang.tr("(empty)", "(trống)") : String.join(", ", names))));

        return 1;
    }

    private static int help(CommandContext<FabricClientCommandSource> context)
    {
        String[] lines = Lang.vietnamese() ? new String[]
        {
            "/backdrop banner|wallpaper video <tên file> | url <link mp4> | on | off | clear | volume 0-1 | sync <ms>",
            "/backdrop videos  (liệt kê video trong assets/video)",
            "/backdrop darkness 0-1  ·  /backdrop panels 0-1  ·  /backdrop ffmpeg <đường dẫn>",
            "/backdrop spotify id <ClientID> | login | logout | play | pause | next | prev | volume <0-100> | card true|false"
        } : new String[]
        {
            "/backdrop banner|wallpaper video <file> | url <mp4 link> | on | off | clear | volume 0-1 | sync <ms>",
            "/backdrop videos  (lists the videos in assets/video)",
            "/backdrop darkness 0-1  ·  /backdrop panels 0-1  ·  /backdrop ffmpeg <path>",
            "/backdrop spotify id <ClientID> | login | logout | play | pause | next | prev | volume <0-100> | card true|false"
        };

        for (String line : lines)
        {
            context.getSource().sendFeedback(Text.literal(line));
        }

        return 1;
    }
}
