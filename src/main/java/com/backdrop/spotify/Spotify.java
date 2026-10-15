package com.backdrop.spotify;

import com.backdrop.BackdropAddon;
import com.backdrop.Lang;
import com.backdrop.client.Chat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Spotify, through its Web API: what is playing now, and play / pause / next / previous / volume.
 *
 * <p>Spotify does not let a mod play music itself — it only hands out control of an app that is
 * already playing (the desktop app, the phone). So this is a remote: open Spotify somewhere, start a
 * song, and it shows up here. Reading what plays works for any account; the controls need Premium,
 * which is Spotify's rule, not the addon's.</p>
 *
 * <p>Signing in uses the "PKCE" flow, so no secret is kept in the mod or on the disk: the user
 * makes a free app at developer.spotify.com, puts {@link #REDIRECT} as its Redirect URI, and gives the
 * addon the app's Client ID.</p>
 */
public class Spotify
{
    public static final String REDIRECT = "http://127.0.0.1:8977/callback";

    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing";
    private static final long POLL_MS = 1500;
    private static final long IDLE_NANOS = 10_000_000_000L;

    /* What the card shows */
    public static volatile boolean playing;
    public static volatile boolean active;
    public static volatile String title = "";
    public static volatile String artist = "";
    public static volatile String status = "";
    public static volatile long progressMs;
    public static volatile long durationMs;
    public static volatile int volume = -1;
    public static volatile BufferedImage pendingCover;

    private static volatile String coverUrl = "";
    private static volatile long stateAt;
    private static volatile long wanted;
    private static volatile boolean pollNow;
    private static Thread poller;
    private static volatile boolean loggingIn;

    /** The card is on screen: keep the state fresh. */
    public static synchronized void wanted()
    {
        wanted = System.nanoTime();

        if (poller == null || !poller.isAlive())
        {
            poller = new Thread(Spotify::pollLoop, "Backdrop Spotify");
            poller.setDaemon(true);
            poller.start();
        }
    }

    public static boolean configured()
    {
        return BackdropAddon.spotifyClientId != null && !BackdropAddon.spotifyClientId.get().trim().isEmpty();
    }

    public static boolean signedIn()
    {
        return BackdropAddon.spotifyRefreshToken != null && !BackdropAddon.spotifyRefreshToken.get().isEmpty();
    }

    /** Progress as it is now: the last reading, carried on by the clock while it plays. */
    public static long progress()
    {
        long value = progressMs;

        if (playing)
        {
            value += (System.nanoTime() - stateAt) / 1_000_000L;
        }

        return Math.max(0L, durationMs > 0 ? Math.min(value, durationMs) : value);
    }

    /* ---- Commands ---- */

    public static void playPause()
    {
        send(playing ? "PUT" : "PUT", playing ? "/me/player/pause" : "/me/player/play");
    }

    public static void next()
    {
        send("POST", "/me/player/next");
    }

    public static void previous()
    {
        send("POST", "/me/player/previous");
    }

    public static void setVolume(int percent)
    {
        send("PUT", "/me/player/volume?volume_percent=" + Math.max(0, Math.min(100, percent)));
    }

    private static void send(String method, String path)
    {
        Thread thread = new Thread(() ->
        {
            try
            {
                Response response = api(method, path);

                if (response.code == 403)
                {
                    status = Lang.tr("Spotify Premium is needed to control playback", "Cần Spotify Premium để điều khiển phát nhạc");
                }
                else if (response.code == 404)
                {
                    status = Lang.tr("No active device: start Spotify on a device first", "Chưa có thiết bị nào đang mở: hãy mở Spotify rồi phát một bài");
                }
                else if (response.code >= 400)
                {
                    status = "Spotify: HTTP " + response.code;
                }

                Thread.sleep(350);
                pollNow = true;
            }
            catch (Exception e)
            {
                status = "Spotify: " + e.getMessage();
            }
        }, "Backdrop Spotify command");

        thread.setDaemon(true);
        thread.start();
    }

    /* ---- Polling ---- */

    private static void pollLoop()
    {
        while (System.nanoTime() - wanted < IDLE_NANOS)
        {
            try
            {
                if (signedIn())
                {
                    poll();
                }
            }
            catch (Exception e)
            {
                status = "Spotify: " + e.getMessage();
            }

            long until = System.currentTimeMillis() + POLL_MS;

            while (System.currentTimeMillis() < until && !pollNow)
            {
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                    return;
                }
            }

            pollNow = false;
        }
    }

    private static void poll() throws Exception
    {
        Response response = api("GET", "/me/player");

        if (response.code == 204 || response.body.isEmpty())
        {
            active = false;
            playing = false;
            status = Lang.tr("Nothing playing: start Spotify on a device", "Chưa phát gì: hãy mở Spotify trên một thiết bị rồi phát nhạc");

            return;
        }

        if (response.code != 200)
        {
            status = "Spotify: HTTP " + response.code;

            return;
        }

        JsonObject root = JsonParser.parseString(response.body).getAsJsonObject();

        active = true;
        status = "";
        playing = root.has("is_playing") && root.get("is_playing").getAsBoolean();
        progressMs = root.has("progress_ms") && !root.get("progress_ms").isJsonNull() ? root.get("progress_ms").getAsLong() : 0L;
        stateAt = System.nanoTime();

        if (root.has("device") && root.get("device").isJsonObject())
        {
            JsonElement level = root.getAsJsonObject("device").get("volume_percent");

            volume = level == null || level.isJsonNull() ? -1 : level.getAsInt();
        }

        if (!root.has("item") || !root.get("item").isJsonObject())
        {
            title = "";
            artist = "";
            durationMs = 0;

            return;
        }

        JsonObject item = root.getAsJsonObject("item");

        title = item.has("name") ? item.get("name").getAsString() : "";
        durationMs = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L;

        StringBuilder names = new StringBuilder();

        if (item.has("artists") && item.get("artists").isJsonArray())
        {
            for (JsonElement element : item.getAsJsonArray("artists"))
            {
                if (names.length() > 0)
                {
                    names.append(", ");
                }

                names.append(element.getAsJsonObject().get("name").getAsString());
            }
        }

        artist = names.toString();

        if (item.has("album") && item.get("album").isJsonObject())
        {
            String url = pickCover(item.getAsJsonObject("album").getAsJsonArray("images"));

            if (url != null && !url.equals(coverUrl))
            {
                coverUrl = url;
                loadCover(url);
            }
        }
    }

    /** The smallest picture that is still at least 64 pixels wide (the card draws it at 48). */
    private static String pickCover(JsonArray images)
    {
        if (images == null || images.size() == 0)
        {
            return null;
        }

        String best = null;
        int bestWidth = Integer.MAX_VALUE;

        for (JsonElement element : images)
        {
            JsonObject image = element.getAsJsonObject();
            int width = image.has("width") && !image.get("width").isJsonNull() ? image.get("width").getAsInt() : 0;

            if (width >= 64 && width < bestWidth)
            {
                bestWidth = width;
                best = image.get("url").getAsString();
            }
        }

        return best != null ? best : images.get(images.size() - 1).getAsJsonObject().get("url").getAsString();
    }

    private static void loadCover(String url)
    {
        Thread thread = new Thread(() ->
        {
            try
            {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);

                try (InputStream stream = connection.getInputStream())
                {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(stream.readAllBytes()));

                    if (image != null)
                    {
                        pendingCover = image;
                    }
                }
            }
            catch (Exception ignored)
            {}
        }, "Backdrop Spotify cover");

        thread.setDaemon(true);
        thread.start();
    }

    /* ---- The web ---- */

    private static class Response
    {
        public final int code;
        public final String body;

        public Response(int code, String body)
        {
            this.code = code;
            this.body = body;
        }
    }

    /** A call to the Web API with the access token, refreshed when it ran out (and once more if Spotify says it did). */
    private static Response api(String method, String path) throws Exception
    {
        for (int attempt = 0; attempt < 2; attempt++)
        {
            String token = token(attempt > 0);

            if (token == null)
            {
                return new Response(401, "");
            }

            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spotify.com/v1" + path).openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("Authorization", "Bearer " + token);

            if (!method.equals("GET"))
            {
                /* Spotify wants a length on the commands that carry no body */
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(0);
                connection.getOutputStream().close();
            }

            Response response = read(connection);

            if (response.code != 401)
            {
                return response;
            }
        }

        status = Lang.tr("Spotify session ended: sign in again in the Backdrop panel", "Phiên Spotify đã hết: đăng nhập lại trong bảng Backdrop");

        return new Response(401, "");
    }

    private static Response read(HttpURLConnection connection) throws Exception
    {
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = "";

        if (stream != null)
        {
            try (InputStream in = stream)
            {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return new Response(code, body);
    }

    /** The access token, asking for a new one when the old one is (about to be) out of date. */
    private static String token(boolean force) throws Exception
    {
        String refresh = BackdropAddon.spotifyRefreshToken.get();

        if (refresh.isEmpty())
        {
            return null;
        }

        long expiresAt = 0;

        try
        {
            expiresAt = Long.parseLong(BackdropAddon.spotifyExpiresAt.get());
        }
        catch (NumberFormatException ignored)
        {}

        if (force || BackdropAddon.spotifyAccessToken.get().isEmpty() || System.currentTimeMillis() > expiresAt - 30_000L)
        {
            Response response = form("grant_type=refresh_token&refresh_token=" + enc(refresh) + "&client_id=" + enc(BackdropAddon.spotifyClientId.get().trim()));

            if (response.code != 200)
            {
                return null;
            }

            storeTokens(JsonParser.parseString(response.body).getAsJsonObject());
        }

        return BackdropAddon.spotifyAccessToken.get();
    }

    private static Response form(String body) throws Exception
    {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://accounts.spotify.com/api/token").openConnection();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setFixedLengthStreamingMode(bytes.length);

        try (OutputStream out = connection.getOutputStream())
        {
            out.write(bytes);
        }

        return read(connection);
    }

    private static void storeTokens(JsonObject json)
    {
        BackdropAddon.spotifyAccessToken.set(json.get("access_token").getAsString());
        BackdropAddon.spotifyExpiresAt.set(String.valueOf(System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000L));

        if (json.has("refresh_token") && !json.get("refresh_token").isJsonNull())
        {
            BackdropAddon.spotifyRefreshToken.set(json.get("refresh_token").getAsString());
        }
    }

    private static String enc(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /* ---- Signing in ---- */

    public static void logout()
    {
        BackdropAddon.spotifyAccessToken.set("");
        BackdropAddon.spotifyRefreshToken.set("");
        BackdropAddon.spotifyExpiresAt.set("0");

        active = false;
        playing = false;
        title = "";
        artist = "";
    }

    /** Opens Spotify's page in the browser and waits (in the background) for it to come back to this computer. */
    public static void login()
    {
        if (!configured())
        {
            Chat.say(Lang.tr("Set your Client ID first (Backdrop panel or settings)", "Hãy đặt Client ID trước (trong bảng Backdrop hoặc Cài đặt)"));

            return;
        }

        if (loggingIn)
        {
            Chat.say(Lang.tr("Already waiting for the browser...", "Đang chờ trình duyệt rồi..."));

            return;
        }

        loggingIn = true;

        Thread thread = new Thread(() ->
        {
            try (ServerSocket server = new ServerSocket(8977, 1, InetAddress.getByName("127.0.0.1")))
            {
                SecureRandom random = new SecureRandom();
                byte[] raw = new byte[64];

                random.nextBytes(raw);

                String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                String state = Long.toHexString(random.nextLong());
                String url = "https://accounts.spotify.com/authorize?response_type=code"
                    + "&client_id=" + enc(BackdropAddon.spotifyClientId.get().trim())
                    + "&scope=" + enc(SCOPES)
                    + "&redirect_uri=" + enc(REDIRECT)
                    + "&state=" + state
                    + "&code_challenge_method=S256&code_challenge=" + challenge;

                Chat.say(Lang.tr("Opening Spotify in your browser...", "Đang mở Spotify trong trình duyệt..."));
                Util.getOperatingSystem().open(URI.create(url));

                server.setSoTimeout(180_000);

                String code = null;

                try (Socket socket = server.accept())
                {
                    String request = readRequestLine(socket.getInputStream());
                    String reply = Lang.tr("Done! You can close this tab and go back to Minecraft.", "Xong! Bạn có thể đóng tab này và quay lại Minecraft.");
                    byte[] page = ("<html><body style=\"font-family:sans-serif\"><h2>" + reply + "</h2></body></html>").getBytes(StandardCharsets.UTF_8);

                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + page.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(page);
                    socket.getOutputStream().flush();

                    code = queryValue(request, "code");

                    if (!state.equals(queryValue(request, "state")))
                    {
                        code = null;
                    }
                }

                if (code == null)
                {
                    Chat.say(Lang.tr("Spotify sign-in was refused or cancelled.", "Đăng nhập Spotify bị từ chối hoặc đã hủy."));

                    return;
                }

                Response response = form("grant_type=authorization_code&code=" + enc(code)
                    + "&redirect_uri=" + enc(REDIRECT)
                    + "&client_id=" + enc(BackdropAddon.spotifyClientId.get().trim())
                    + "&code_verifier=" + enc(verifier));

                if (response.code != 200)
                {
                    Chat.say(Lang.tr("Spotify did not accept the sign-in (HTTP " + response.code + "). Check the Client ID and the Redirect URI.",
                        "Spotify không chấp nhận đăng nhập (HTTP " + response.code + "). Kiểm tra Client ID và Redirect URI."));

                    return;
                }

                storeTokens(JsonParser.parseString(response.body).getAsJsonObject());
                Chat.say(Lang.tr("Signed in to Spotify.", "Đã đăng nhập Spotify."));
                pollNow = true;
            }
            catch (java.net.BindException e)
            {
                Chat.say(Lang.tr("Port 8977 is busy; close whatever uses it and try again.", "Cổng 8977 đang bận; hãy tắt chương trình đang dùng nó rồi thử lại."));
            }
            catch (java.net.SocketTimeoutException e)
            {
                Chat.say(Lang.tr("Spotify sign-in timed out.", "Đăng nhập Spotify quá thời gian."));
            }
            catch (Exception e)
            {
                Chat.say("Spotify: " + e.getMessage());
            }
            finally
            {
                loggingIn = false;
            }
        }, "Backdrop Spotify login");

        thread.setDaemon(true);
        thread.start();
    }

    private static String readRequestLine(InputStream stream) throws Exception
    {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;

        while ((value = stream.read()) >= 0 && value != '\n' && line.size() < 8192)
        {
            if (value != '\r')
            {
                line.write(value);
            }
        }

        return line.toString(StandardCharsets.UTF_8);
    }

    /** The value of a query parameter in "GET /callback?code=...&state=... HTTP/1.1". */
    private static String queryValue(String request, String name)
    {
        int question = request.indexOf('?');

        if (question < 0)
        {
            return null;
        }

        String query = request.substring(question + 1);
        int space = query.indexOf(' ');

        if (space >= 0)
        {
            query = query.substring(0, space);
        }

        for (String pair : query.split("&"))
        {
            int equals = pair.indexOf('=');

            if (equals > 0 && pair.substring(0, equals).equals(name))
            {
                return java.net.URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
            }
        }

        return null;
    }
}
