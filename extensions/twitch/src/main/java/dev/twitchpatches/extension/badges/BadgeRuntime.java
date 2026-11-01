package dev.twitchpatches.extension.badges;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import dev.twitchpatches.extension.emotes.Emote;
import dev.twitchpatches.extension.emotes.EmoteImages;
import dev.twitchpatches.extension.settings.PatchSettings;
import dev.twitchpatches.extension.shared.ReactNativeRuntime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class BadgeRuntime {
    private static final String MARKER = "twitchpatches-badge:";
    private static final int REACT_NATIVE_FEATURE = 10;
    private static volatile BadgeRuntime instance;
    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<BadgeSpan> spans = Collections.newSetFromMap(new WeakHashMap<>());
    private final BadgeCatalog catalog = new BadgeCatalog(this::refreshOnMain);
    private final SevenTvBadges sevenTv = new SevenTvBadges(this::refreshOnMain);
    private final EmoteImages images = new EmoteImages(url -> main.post(() -> refreshImage(url)), 2, 4);
    private volatile int providerMask;

    private BadgeRuntime(Application application) {
        preferences = application.getSharedPreferences("twitch_patches_badges", 0);
        providerMask = preferences.getInt("provider_mask", BadgeProviders.ALL) & BadgeProviders.ALL;
    }

    public static synchronized void initialize(Application application) {
        if (instance != null) return;
        instance = new BadgeRuntime(application);
        PatchSettings.register(new BadgeSetting());
        ReactNativeRuntime.select(REACT_NATIVE_FEATURE);
    }

    public static boolean providerEnabled(int provider) {
        BadgeRuntime runtime = instance;
        return runtime != null && (runtime.providerMask & (1 << provider)) != 0;
    }

    static int enabledCount() {
        BadgeRuntime runtime = instance;
        return runtime == null ? 0 : Integer.bitCount(runtime.providerMask);
    }

    static void setAllEnabled(boolean enabled) { setMask(enabled ? BadgeProviders.ALL : 0); }

    static void setProviderEnabled(int provider, boolean enabled) {
        BadgeRuntime runtime = instance;
        if (runtime == null) return;
        int bit = 1 << provider;
        setMask(enabled ? runtime.providerMask | bit : runtime.providerMask & ~bit);
    }

    private static void setMask(int mask) {
        BadgeRuntime runtime = instance;
        if (runtime == null) return;
        runtime.providerMask = mask;
        runtime.preferences.edit().putInt("provider_mask", mask).apply();
        ReactNativeRuntime.refresh();
        runtime.refreshOnMain();
    }

    public static List<?> withRowBadge(List<?> badges, Object sender) {
        BadgeRuntime runtime = instance;
        long user = BadgeProviders.userId(BadgeBridge.senderId(sender));
        if (runtime == null || user == 0) return badges;
        List<Object> result = new ArrayList<>(badges);
        result.add(BadgeBridge.model(MARKER, MARKER + user));
        return result;
    }

    public static SpannableString rowBadgeText(String url) {
        if (url == null || !url.startsWith(MARKER)) return null;
        BadgeRuntime runtime = instance;
        long user = BadgeProviders.userId(url.substring(MARKER.length()));
        if (runtime == null || user == 0) return null;
        BadgeSpan span = new BadgeSpan(user);
        synchronized (runtime.spans) { runtime.spans.add(span); }
        badgeFor(user);
        SpannableString text = new SpannableString("￼ ");
        text.setSpan(span, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    static Badge badgeFor(long user) {
        BadgeRuntime runtime = instance;
        if (runtime == null) return null;
        for (int provider = 0; provider < BadgeProviders.COUNT; provider++) {
            if ((runtime.providerMask & (1 << provider)) == 0) continue;
            Badge badge = runtime.find(provider, user);
            if (badge != null) return badge;
        }
        return null;
    }

    static Drawable image(Resources resources, String url) {
        BadgeRuntime runtime = instance;
        if (runtime == null) return null;
        Drawable drawable = runtime.images.drawable(resources, url);
        if (drawable == null) runtime.images.request(new Emote("", url, false, false));
        return drawable;
    }

    private Badge find(int provider, long user) {
        if (provider == BadgeProviders.SEVEN_TV) return sevenTv.find(user);
        catalog.ensure(provider);
        return catalog.find(provider, user);
    }

    private void refreshOnMain() { main.post(() -> { for (BadgeSpan span : liveSpans()) span.relayout(); }); }

    private void refreshImage(String url) {
        for (BadgeSpan span : liveSpans()) if (span.shows(url)) span.relayout();
    }

    private List<BadgeSpan> liveSpans() {
        synchronized (spans) { return new ArrayList<>(spans); }
    }
}
