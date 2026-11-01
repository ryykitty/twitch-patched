(function (runtime) {
    'use strict';
    var POLICY_START = 15, SEVEN_TV = 0, BATCH_SIZE = 25, QUEUE_LIMIT = 500, CACHE_LIMIT = 4096;
    var BATCH_DELAY = 750, REFRESH_AFTER = 3600000, RETRY_AFTER = 60000, KEEP_LOOKUP = 1800000, SIZE_LIMIT = 8388608;
    var providers = ['7tv', 'dankchat', 'chatterino', 'ffz'];
    var catalogURLs = [null, 'https://flxrs.com/api/badges', 'https://api.chatterino.com/badges', 'https://api.frankerfacez.com/v1/badges/ids'];
    var tables = [null, new Map(), new Map(), new Map()], nextLoad = [0, 0, 0, 0];
    var lookups = new Map(), queue = new Set(), batchScheduled = false, retryLookupsAt = 0;
    var listeners = new Set(), revision = 0;

    function changed() { revision++; listeners.forEach(function (listener) { listener(); }); }
    function subscribe(listener) { listeners.add(listener); return function () { listeners.delete(listener); }; }
    function currentRevision() { return revision; }
    function enabledMask() {
        return providers.reduce(function (mask, provider, index) { return mask | (runtime.enabled(POLICY_START + index) ? 1 << index : 0); }, 0);
    }
    function safeURL(url) {
        return typeof url === 'string' && url.length < 512 &&
            /^https:\/\/(cdn\.7tv\.(app|io)|api\.dank\.chat|fourtf\.com|cdn\.frankerfacez\.com)\/[A-Za-z0-9_./-]+$/.test(url);
    }
    function background(color) { return typeof color === 'string' && /^#[0-9a-fA-F]{6}$/.test(color) ? color : null; }
    function table(provider, groups) {
        var result = new Map();
        groups.filter(function (group) { return Array.isArray(group.users) && safeURL(group.url); })
            .sort(function (first, second) { return second.users.length - first.users.length; })
            .forEach(function (group, index) {
                var badge = {set: 'twitchpatches-' + provider, version: String(index), background: group.background || null,
                    image: {image1x: group.url, image2x: group.url, image4x: group.url}};
                group.users.forEach(function (user) { result.set(String(user), badge); });
            });
        return result;
    }
    function parseDankChat(data) {
        return table('dankchat', (Array.isArray(data) ? data : []).map(function (entry) { return {url: entry.url, users: entry.users}; }));
    }
    function parseChatterino(data) {
        return table('chatterino', (data && Array.isArray(data.badges) ? data.badges : []).map(function (entry) {
            return {url: entry.image3, users: entry.users};
        }));
    }
    function parseFrankerFaceZ(data) {
        var users = data && data.users || {};
        return table('ffz', (data && Array.isArray(data.badges) ? data.badges : []).map(function (entry) {
            return {url: entry.urls && entry.urls['2'], background: background(entry.color), users: users[entry.id]};
        }));
    }
    var parsers = [null, parseDankChat, parseChatterino, parseFrankerFaceZ];
    function ensureCatalog(index) {
        if (Date.now() < nextLoad[index]) return;
        nextLoad[index] = Date.now() + RETRY_AFTER;
        fetch(catalogURLs[index], {credentials: 'omit', redirect: 'error'}).then(function (response) {
            if (!response.ok) throw new Error('Badge provider unavailable');
            return response.text();
        }).then(function (text) {
            if (text.length > SIZE_LIMIT) throw new Error('Badge catalog exceeds bounds');
            tables[index] = parsers[index](JSON.parse(text));
            nextLoad[index] = Date.now() + REFRESH_AFTER;
            runtime.log('badges catalog provider=' + providers[index] + ' count=' + tables[index].size);
            changed();
        }).catch(function () { runtime.log('badges catalog unavailable provider=' + providers[index]); });
    }
    function lookupQuery(users) {
        return 'query { users {' + users.map(function (user) {
            return ' u' + user + ': userByConnection(platform: TWITCH, platformId: "' + user +
                '") { style { activeBadge { images { url mime scale frameCount } } } }';
        }).join('') + ' } }';
    }
    function sevenTvBadge(entry) {
        var images = entry && entry.style && entry.style.activeBadge && entry.style.activeBadge.images;
        var image = Array.isArray(images) && images.find(function (candidate) {
            return candidate.scale === 2 && candidate.frameCount === 1 && candidate.mime === 'image/webp' && safeURL(candidate.url);
        });
        return image ? {set: 'twitchpatches-7tv', version: image.url, background: null,
            image: {image1x: image.url, image2x: image.url, image4x: image.url}} : null;
    }
    function storeLookup(user, badge) {
        if (lookups.size >= CACHE_LIMIT) lookups.delete(lookups.keys().next().value);
        lookups.set(user, {badge: badge, time: Date.now()});
    }
    function requestBatch() {
        var users = Array.from(queue).slice(0, BATCH_SIZE);
        users.forEach(function (user) { queue.delete(user); });
        fetch('https://7tv.io/v4/gql', {method: 'POST', credentials: 'omit', redirect: 'error', headers: {'content-type': 'application/json'},
            body: JSON.stringify({query: lookupQuery(users)})}).then(function (response) {
            if (!response.ok) throw new Error('7TV lookup unavailable');
            return response.json();
        }).then(function (result) {
            users.forEach(function (user) { storeLookup(user, sevenTvBadge(result.data.users['u' + user])); });
            changed();
        }).catch(function () {
            retryLookupsAt = Date.now() + RETRY_AFTER;
            runtime.log('badges lookup unavailable');
        }).finally(function () {
            batchScheduled = false;
            scheduleBatch();
        });
    }
    function scheduleBatch() {
        if (batchScheduled || !queue.size) return;
        batchScheduled = true;
        setTimeout(requestBatch, Math.max(BATCH_DELAY, retryLookupsAt - Date.now()));
    }
    function validUser(user) { return typeof user === 'string' && /^[1-9][0-9]{0,17}$/.test(user); }
    function requestBadges(user, mask) {
        for (var index = 1; index < providers.length; index++) if (mask & (1 << index)) ensureCatalog(index);
        if (!(mask & (1 << SEVEN_TV)) || !validUser(user)) return;
        var lookup = lookups.get(user);
        if (lookup && Date.now() - lookup.time < KEEP_LOOKUP) return;
        if (queue.size < QUEUE_LIMIT) queue.add(user);
        scheduleBatch();
    }
    function badgeFor(user, mask) {
        if (!validUser(user)) return null;
        for (var index = 0; index < providers.length; index++) {
            if (!(mask & (1 << index))) continue;
            var badge = index === SEVEN_TV ? (lookups.get(user) || {}).badge : tables[index].get(user);
            if (badge) return badge;
        }
        return null;
    }
    runtime.decorateType(__TWITCH_BADGE_ROW_MODULE__, 'ChatMessageRow', function (original, React) {
        return function (props) {
            var mask = React.useSyncExternalStore(runtime.subscribe, enabledMask, enabledMask);
            React.useSyncExternalStore(subscribe, currentRevision, currentRevision);
            var user = props.message && props.message.userId;
            React.useEffect(function () { if (mask) requestBadges(user, mask); }, [user, mask]);
            var badge = mask ? badgeFor(user, mask) : null;
            var next = React.useMemo(function () {
                if (!badge) return props;
                var registry = Object.create(props.badgeRegistry || null);
                registry[badge.set + '/' + badge.version] = Object.assign({background: badge.background}, badge.image);
                var badges = Object.assign({}, props.message.badges);
                badges[badge.set] = badge.version;
                return Object.assign({}, props, {badgeRegistry: registry, message: Object.assign({}, props.message, {badges: badges})});
            }, [props, badge]);
            return original.call(this, next);
        };
    });
    runtime.decorate(__TWITCH_BADGE_PART_MODULE__, 'BadgePart', function (original, React) {
        var PREFIX = 'chat-badge-twitchpatches-ffz';
        function tinted(node, color, depth) {
            if (!React.isValidElement(node) || depth > 4) return node;
            if (typeof node.props.testID === 'string' && node.props.testID.indexOf(PREFIX) === 0)
                return React.cloneElement(node, {}, React.createElement(node.type,
                    {style: {alignSelf: 'flex-start', backgroundColor: color, borderRadius: 3}}, node.props.children));
            if (node.props.children == null) return node;
            return React.cloneElement(node, {}, React.Children.map(node.props.children, function (child) { return tinted(child, color, depth + 1); }));
        }
        return function BadgePart(props) {
            var tree = original(props);
            var version = props.badges && props.badges['twitchpatches-ffz'];
            var entry = version != null && props.registry ? props.registry['twitchpatches-ffz/' + version] : null;
            return entry && entry.background ? tinted(tree, entry.background, 0) : tree;
        };
    });
})(globalThis.__twitchPatchRuntime);
