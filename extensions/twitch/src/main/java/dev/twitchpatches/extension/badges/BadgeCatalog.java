package dev.twitchpatches.extension.badges;

import dev.twitchpatches.extension.emotes.EmoteHttp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;

final class BadgeCatalog {
    private static final long REFRESH_MILLIS = 60 * 60 * 1000L;
    private static final long RETRY_MILLIS = 60 * 1000L;
    private static final int SIZE_LIMIT = 8 * 1024 * 1024;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TwitchBadgeCatalog");
        thread.setDaemon(true);
        return thread;
    });
    private final BadgeTable[] tables = new BadgeTable[BadgeProviders.COUNT];
    private final long[] nextLoad = new long[BadgeProviders.COUNT];
    private final Runnable changed;

    BadgeCatalog(Runnable changed) {
        this.changed = changed;
        Arrays.fill(tables, BadgeTable.EMPTY);
    }

    synchronized void ensure(int provider) {
        long now = System.currentTimeMillis();
        if (now < nextLoad[provider]) return;
        nextLoad[provider] = now + RETRY_MILLIS;
        worker.execute(() -> load(provider));
    }

    synchronized Badge find(int provider, long user) { return tables[provider].find(user); }

    private void load(int provider) {
        try {
            byte[] data = EmoteHttp.get(BadgeProviders.catalogUrl(provider), SIZE_LIMIT, false);
            BadgeTable table = BadgeProviders.parse(provider, new String(data, StandardCharsets.UTF_8));
            synchronized (this) {
                tables[provider] = table;
                nextLoad[provider] = System.currentTimeMillis() + REFRESH_MILLIS;
            }
            android.util.Log.i("TwitchPatchesBadges", "Catalog provider=" + provider + " users=" + table.size());
            changed.run();
        } catch (IOException | JSONException error) {
            android.util.Log.w("TwitchPatchesBadges", "Catalog unavailable provider=" + provider);
        }
    }
}
