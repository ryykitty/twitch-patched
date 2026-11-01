package dev.twitchpatches.extension.badges;

import dev.twitchpatches.extension.emotes.EmoteHttp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;

final class SevenTvBadges {
    private static final String ENDPOINT = "https://7tv.io/v4/gql";
    private static final int BATCH_SIZE = 25;
    private static final int QUEUE_LIMIT = 500;
    private static final int CACHE_LIMIT = 4096;
    private static final int RESPONSE_LIMIT = 1024 * 1024;
    private static final long BATCH_DELAY_MILLIS = 750;
    private static final long KEEP_MILLIS = 30 * 60 * 1000L;
    private static final long RETRY_MILLIS = 60 * 1000L;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "TwitchBadgeLookups");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Long, Lookup> results = new LinkedHashMap<Long, Lookup>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, Lookup> eldest) { return size() > CACHE_LIMIT; }
    };
    private final Set<Long> queue = new LinkedHashSet<>();
    private final Runnable changed;
    private boolean scheduled;
    private long retryAfter;

    SevenTvBadges(Runnable changed) { this.changed = changed; }

    synchronized Badge find(long user) {
        Lookup lookup = results.get(user);
        if (lookup != null && System.currentTimeMillis() - lookup.time < KEEP_MILLIS) return lookup.badge;
        if (queue.size() < QUEUE_LIMIT) queue.add(user);
        schedule();
        return lookup == null ? null : lookup.badge;
    }

    private void schedule() {
        if (scheduled || queue.isEmpty()) return;
        scheduled = true;
        long wait = Math.max(BATCH_DELAY_MILLIS, retryAfter - System.currentTimeMillis());
        worker.schedule(this::requestBatch, wait, TimeUnit.MILLISECONDS);
    }

    private void requestBatch() {
        List<Long> users = takeBatch();
        try {
            byte[] data = EmoteHttp.postJson(ENDPOINT, BadgeProviders.sevenTvQuery(users), RESPONSE_LIMIT);
            Map<Long, Badge> found = BadgeProviders.sevenTvBatch(new String(data, StandardCharsets.UTF_8), users);
            store(found);
            changed.run();
        } catch (IOException | JSONException error) {
            synchronized (this) { retryAfter = System.currentTimeMillis() + RETRY_MILLIS; }
            android.util.Log.w("TwitchPatchesBadges", "7TV lookup unavailable");
        }
        synchronized (this) {
            scheduled = false;
            schedule();
        }
    }

    private synchronized List<Long> takeBatch() {
        List<Long> users = new ArrayList<>(BATCH_SIZE);
        Iterator<Long> pending = queue.iterator();
        while (pending.hasNext() && users.size() < BATCH_SIZE) {
            users.add(pending.next());
            pending.remove();
        }
        return users;
    }

    private synchronized void store(Map<Long, Badge> found) {
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, Badge> entry : found.entrySet()) results.put(entry.getKey(), new Lookup(entry.getValue(), now));
    }

    private static final class Lookup {
        final Badge badge;
        final long time;

        Lookup(Badge badge, long time) {
            this.badge = badge;
            this.time = time;
        }
    }
}
