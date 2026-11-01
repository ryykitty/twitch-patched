package dev.twitchpatches.extension.playback;

import android.app.Application;
import android.os.PowerManager;

public final class BackgroundPlayback {
    private static volatile PowerManager power;

    private BackgroundPlayback() { }

    public static void initialize(Application application) {
        power = application.getSystemService(PowerManager.class);
    }

    public static boolean pictureInPictureBlocksAudio(boolean inPictureInPicture) {
        PowerManager manager = power;
        return inPictureInPicture && (manager == null || manager.isInteractive());
    }
}
