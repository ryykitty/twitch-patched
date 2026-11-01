package dev.twitchpatches.extension.badges;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class BadgeTable {
    static final BadgeTable EMPTY = new BadgeTable(new HashMap<>());
    private final long[] users;
    private final Badge[] badges;

    BadgeTable(Map<Long, Badge> assignments) {
        users = new long[assignments.size()];
        badges = new Badge[users.length];
        int index = 0;
        for (long user : assignments.keySet()) users[index++] = user;
        Arrays.sort(users);
        for (index = 0; index < users.length; index++) badges[index] = assignments.get(users[index]);
    }

    Badge find(long user) {
        int index = Arrays.binarySearch(users, user);
        return index < 0 ? null : badges[index];
    }

    int size() { return users.length; }
}
