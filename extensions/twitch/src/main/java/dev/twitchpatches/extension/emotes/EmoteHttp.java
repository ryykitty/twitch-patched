package dev.twitchpatches.extension.emotes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class EmoteHttp {
    public static byte[] get(String url, int limit, boolean optional) throws IOException {
        return read(open(url), limit, optional);
    }

    public static byte[] postJson(String url, String body, int limit) throws IOException {
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return read(connection, limit, false);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "TwitchPatchesEmotes/1.0");
        return connection;
    }

    private static byte[] read(HttpURLConnection connection, int limit, boolean optional) throws IOException {
        try {
            int status = connection.getResponseCode();
            if (status == 404 && optional) return "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (status != 200 || connection.getContentLengthLong() > limit) throw new IOException("Emote request unavailable.");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || output.size() + count > limit) throw new IOException("Emote request cancelled or oversized.");
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        } finally { connection.disconnect(); }
    }

    private EmoteHttp() { }
}
