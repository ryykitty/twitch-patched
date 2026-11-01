package dev.twitchpatches.extension.badges;

public final class BadgeBridge {
    private BadgeBridge() { }

    public static String senderId(Object sender) {
        throw new UnsupportedOperationException("Badge sender bridge is not patched.");
    }

    public static Object model(String name, String url) {
        throw new UnsupportedOperationException("Badge model bridge is not patched.");
    }
}
