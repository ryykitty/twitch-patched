package dev.twitchpatches.extension.badges;

import dev.twitchpatches.extension.settings.SettingGroup;
import dev.twitchpatches.extension.settings.SettingSection;
import dev.twitchpatches.extension.settings.ToggleSetting;
import java.util.Arrays;
import java.util.List;

final class BadgeSetting implements SettingGroup {
    @Override public String key() { return "third_party_badges"; }
    @Override public String title() { return "Badges"; }
    @Override public String summary() { return "Shows one 7TV, DankChat, Chatterino or FrankerFaceZ badge next to the Twitch badges in chat."; }
    @Override public SettingSection section() { return SettingSection.CHAT; }
    @Override public List<ToggleSetting> children() {
        return Arrays.asList(new AllProvidersSetting(), new ProviderSetting(BadgeProviders.SEVEN_TV, "7TV"),
                new ProviderSetting(BadgeProviders.DANKCHAT, "DankChat"), new ProviderSetting(BadgeProviders.CHATTERINO, "Chatterino"),
                new ProviderSetting(BadgeProviders.FRANKERFACEZ, "FrankerFaceZ"));
    }

    private static final class AllProvidersSetting implements ToggleSetting {
        @Override public String key() { return "third_party_badges"; }
        @Override public String title() { return "Enable all providers"; }
        @Override public String summary() {
            int count = BadgeRuntime.enabledCount();
            if (count == BadgeProviders.COUNT) return "All providers enabled. Someone with several badges shows the first enabled one in this list.";
            return count == 0 ? "All providers disabled." : count + " of " + BadgeProviders.COUNT + " providers enabled.";
        }
        @Override public SettingSection section() { return SettingSection.CHAT; }
        @Override public boolean isEnabled() { return BadgeRuntime.enabledCount() == BadgeProviders.COUNT; }
        @Override public void setEnabled(boolean enabled) { BadgeRuntime.setAllEnabled(enabled); }
    }

    private static final class ProviderSetting implements ToggleSetting {
        private final int provider;
        private final String title;

        ProviderSetting(int provider, String title) {
            this.provider = provider;
            this.title = title;
        }

        @Override public String key() { return "badge_provider_" + provider; }
        @Override public String title() { return title; }
        @Override public String summary() { return ""; }
        @Override public SettingSection section() { return SettingSection.CHAT; }
        @Override public boolean isEnabled() { return BadgeRuntime.providerEnabled(provider); }
        @Override public void setEnabled(boolean enabled) { BadgeRuntime.setProviderEnabled(provider, enabled); }
    }
}
