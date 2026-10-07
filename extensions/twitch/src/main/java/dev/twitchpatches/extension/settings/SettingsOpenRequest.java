package dev.twitchpatches.extension.settings;

final class SettingsOpenRequest {
    private boolean pending;

    void request() { pending = true; }

    boolean consume(boolean resumed, boolean alreadyOpen) {
        if (!pending || !resumed) return false;
        pending = false;
        return !alreadyOpen;
    }
}
