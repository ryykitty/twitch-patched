package dev.twitchpatches.extension.badges;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import org.json.JSONException;
import org.junit.Test;

public class BadgeProvidersTest {
    @Test public void frankerFaceZKeepsTheRarestBadgeAndItsColor() throws JSONException {
        BadgeTable table = BadgeProviders.frankerFaceZ("{\"badges\":["
                + "{\"id\":3,\"color\":\"#755000\",\"urls\":{\"2\":\"https://cdn.frankerfacez.com/badge/3/2\"}},"
                + "{\"id\":1,\"color\":\"#FAAF19\",\"urls\":{\"2\":\"https://cdn.frankerfacez.com/badge/1/2\"}}],"
                + "\"users\":{\"3\":[11,22,33],\"1\":[22]}}");
        assertEquals("https://cdn.frankerfacez.com/badge/3/2", table.find(11).url);
        assertEquals("https://cdn.frankerfacez.com/badge/1/2", table.find(22).url);
        assertEquals(0xFFFAAF19, table.find(22).background);
        assertNull(table.find(44));
    }

    @Test public void chatterinoAndDankChatReadStringUserIds() throws JSONException {
        BadgeTable chatterino = BadgeProviders.chatterino("{\"badges\":[{\"image3\":\"https://fourtf.com/chatterino/badges/dev3x.png\",\"users\":[\"5\"]}]}");
        assertEquals("https://fourtf.com/chatterino/badges/dev3x.png", chatterino.find(5).url);
        assertEquals(0, chatterino.find(5).background);
        BadgeTable dankChat = BadgeProviders.dankChat("[{\"url\":\"https://api.dank.chat/badges/dank.png\",\"users\":[\"6\",\"not a number\"]}]");
        assertEquals(1, dankChat.size());
        assertEquals("https://api.dank.chat/badges/dank.png", dankChat.find(6).url);
    }

    @Test public void imagesOnOtherHostsAreRejected() throws JSONException {
        BadgeTable table = BadgeProviders.dankChat("[{\"url\":\"https://example.test/a.png\",\"users\":[\"6\"]},"
                + "{\"url\":\"http://api.dank.chat/a.png\",\"users\":[\"7\"]}]");
        assertEquals(0, table.size());
    }

    @Test public void sevenTvBatchRecordsUsersWithoutBadgesAndPicksTheStaticImage() throws JSONException {
        Map<Long, Badge> found = BadgeProviders.sevenTvBatch("{\"data\":{\"users\":{\"u1\":null,\"u2\":{\"style\":{\"activeBadge\":null}},"
                + "\"u3\":{\"style\":{\"activeBadge\":{\"images\":["
                + "{\"url\":\"https://cdn.7tv.app/badge/a/2x.webp\",\"mime\":\"image/webp\",\"scale\":2,\"frameCount\":200},"
                + "{\"url\":\"https://cdn.7tv.app/badge/a/1x_static.webp\",\"mime\":\"image/webp\",\"scale\":1,\"frameCount\":1},"
                + "{\"url\":\"https://cdn.7tv.app/badge/a/2x_static.webp\",\"mime\":\"image/webp\",\"scale\":2,\"frameCount\":1}]}}}}}}",
                Arrays.asList(1L, 2L, 3L));
        assertEquals(3, found.size());
        assertTrue(found.containsKey(1L));
        assertNull(found.get(1L));
        assertNull(found.get(2L));
        assertEquals("https://cdn.7tv.app/badge/a/2x_static.webp", found.get(3L).url);
    }

    @Test public void onlyNumericIdsReachTheLookupQuery() {
        assertEquals(0, BadgeProviders.userId("1\") { evil }"));
        assertEquals(0, BadgeProviders.userId("0"));
        assertEquals(42, BadgeProviders.userId("42"));
        assertTrue(BadgeProviders.sevenTvQuery(Arrays.asList(42L)).contains("u42: userByConnection(platform: TWITCH, platformId: \\\"42\\\")"));
    }
}
