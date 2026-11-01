package dev.twitchpatches.extension.badges;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class BadgeProviders {
    static final int SEVEN_TV = 0;
    static final int DANKCHAT = 1;
    static final int CHATTERINO = 2;
    static final int FRANKERFACEZ = 3;
    static final int COUNT = 4;
    static final int ALL = (1 << COUNT) - 1;
    private static final Set<String> IMAGE_HOSTS = new HashSet<>(Arrays.asList("cdn.7tv.app", "cdn.7tv.io",
            "api.dank.chat", "fourtf.com", "cdn.frankerfacez.com"));

    private BadgeProviders() { }

    static String catalogUrl(int provider) {
        if (provider == DANKCHAT) return "https://flxrs.com/api/badges";
        if (provider == CHATTERINO) return "https://api.chatterino.com/badges";
        return "https://api.frankerfacez.com/v1/badges/ids";
    }

    static BadgeTable parse(int provider, String source) throws JSONException {
        if (provider == DANKCHAT) return dankChat(source);
        if (provider == CHATTERINO) return chatterino(source);
        return frankerFaceZ(source);
    }

    static BadgeTable dankChat(String source) throws JSONException {
        JSONArray entries = new JSONArray(source);
        List<Group> groups = new ArrayList<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject entry = entries.getJSONObject(index);
            groups.add(new Group(entry.optString("url"), 0, entry.optJSONArray("users")));
        }
        return table(groups);
    }

    static BadgeTable chatterino(String source) throws JSONException {
        JSONArray entries = new JSONObject(source).getJSONArray("badges");
        List<Group> groups = new ArrayList<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject entry = entries.getJSONObject(index);
            groups.add(new Group(entry.optString("image3"), 0, entry.optJSONArray("users")));
        }
        return table(groups);
    }

    static BadgeTable frankerFaceZ(String source) throws JSONException {
        JSONObject root = new JSONObject(source);
        JSONArray entries = root.getJSONArray("badges");
        JSONObject users = root.getJSONObject("users");
        List<Group> groups = new ArrayList<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject entry = entries.getJSONObject(index);
            JSONObject urls = entry.optJSONObject("urls");
            String url = urls == null ? "" : urls.optString("2");
            groups.add(new Group(url, background(entry.optString("color")), users.optJSONArray(entry.optString("id"))));
        }
        return table(groups);
    }

    static Map<Long, Badge> sevenTvBatch(String source, List<Long> users) throws JSONException {
        JSONObject found = new JSONObject(source).getJSONObject("data").getJSONObject("users");
        Map<Long, Badge> result = new HashMap<>();
        for (long user : users) {
            JSONObject entry = found.optJSONObject("u" + user);
            JSONObject style = entry == null ? null : entry.optJSONObject("style");
            JSONObject badge = style == null ? null : style.optJSONObject("activeBadge");
            String url = badge == null ? null : sevenTvImage(badge.optJSONArray("images"));
            result.put(user, url == null ? null : new Badge(url, 0));
        }
        return result;
    }

    static String sevenTvQuery(List<Long> users) {
        StringBuilder query = new StringBuilder("query { users {");
        for (long user : users) {
            query.append(" u").append(user).append(": userByConnection(platform: TWITCH, platformId: \\\"").append(user)
                    .append("\\\") { style { activeBadge { images { url mime scale frameCount } } } }");
        }
        return "{\"query\":\"" + query.append(" } }") + "\"}";
    }

    private static String sevenTvImage(JSONArray images) {
        if (images == null) return null;
        for (int index = 0; index < images.length(); index++) {
            JSONObject image = images.optJSONObject(index);
            if (image == null || image.optInt("scale") != 2 || image.optInt("frameCount") != 1 ||
                    !"image/webp".equals(image.optString("mime"))) continue;
            String url = image.optString("url");
            if (imageUrl(url)) return url;
        }
        return null;
    }

    static long userId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,17}")) return 0;
        return Long.parseLong(value);
    }

    static boolean imageUrl(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null && uri.getPort() == -1 && IMAGE_HOSTS.contains(uri.getHost());
        } catch (IllegalArgumentException error) { return false; }
    }

    private static int background(String color) {
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) return 0;
        return Integer.parseInt(color.substring(1), 16) | 0xFF000000;
    }

    private static BadgeTable table(List<Group> groups) {
        groups.sort((first, second) -> second.size() - first.size());
        Map<Long, Badge> assignments = new HashMap<>();
        for (Group group : groups) group.assign(assignments);
        return new BadgeTable(assignments);
    }

    private static final class Group {
        private final String url;
        private final int background;
        private final JSONArray users;

        Group(String url, int background, JSONArray users) {
            this.url = url;
            this.background = background;
            this.users = users;
        }

        int size() { return users == null ? 0 : users.length(); }

        void assign(Map<Long, Badge> assignments) {
            if (users == null || !imageUrl(url)) return;
            Badge badge = new Badge(url, background);
            for (int index = 0; index < users.length(); index++) {
                long user = userId(users.optString(index));
                if (user != 0) assignments.put(user, badge);
            }
        }
    }
}
