const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../patches/src/main/resources/reactnative/emote-favorites.js'), 'utf8')
    .replace(/__TWITCH_[A-Z_]+_MODULE__/g, '1');
const state = {card: null};
const React = {
    useMemo(callback) { return callback(); },
    useState() { return [state.card, value => { state.card = value; }]; },
    Fragment: 'Fragment',
    useSyncExternalStore(subscribe, snapshot) { return snapshot(); },
    isValidElement(node) { return Boolean(node && node.props); },
    createElement(type, props) { return {type, key: props && props.key, props: props || {}}; },
    cloneElement(element, props, children) {
        return {...element, key: props && props.key || element.key, props: {...element.props, ...props, ...(arguments.length > 2 ? {children} : {})}};
    },
    Children: {
        toArray(children) { return children == null ? [] : Array.isArray(children) ? children : [children]; },
        map(children, callback) { return this.toArray(children).map(callback); }
    }
};
const twitch = (id, token, extra) => ({id, token, ...extra});
const external = (token, host = 'cdn.7tv.app') => ({id: 'twitchpatches:' + host + '/' + token, token});

function fixture(options = {}) {
    state.card = null;
    const adapters = {}, links = [];
    let favorites, enabled = true, received, preview = options.preview || null, card = options.card || {emoteType: 'global'};
    const runtime = {
        policyHook() { return enabled; },
        enabled() { return enabled; },
        log() {},
        decorate(id, name, adapter) { adapters[name] = adapter; },
        decorateOutermost(id, name, adapter) { adapters[name] = adapter; },
        emoteDetails: {decode(id) {
            const value = id.slice('twitchpatches:'.length), slash = value.indexOf('/');
            return {url: `https://${value.slice(0, slash)}/emote/${value.slice(slash + 1)}/2x.webp`};
        }},
        emotes: options.catalog && {subscribe() { return () => {}; }, pickerSnapshot() { return options.catalog; }},
        emotePicker: options.catalog && {sections: catalog => catalog}
    };
    vm.runInNewContext(source, {globalThis: {__twitchPatchRuntime: runtime, RN$registerCallableModule(name, factory) {
        assert.equal(name, 'TwitchPatchFavorites');
        favorites = factory();
    }}});
    const modules = () => ({
        IconStar: 'IconStar', IconStarHollow: 'IconStarHollow', fetchEmoteCardData: () => card, Pressable: 'Pressable', ChatCardHost: 'ChatCardHost',
        useTheme: () => ({colors: {textBase: 'text'}}),
        Linking: {openURL(url) { links.push(url); return Promise.resolve(); }}
    });
    const hook = adapters.useEmotePreview(() => preview);
    const chosen = [];
    const rowSelect = emote => chosen.push(emote.token);
    const insert = {type: 'MenuItem', props: {testID: 'emote-preview-insert', figure: {icon: 'IconArrowUp'}, children: 'Insert'}};
    const tree = props => {
        received = props;
        hook({});
        return {type: 'Tray', props: {
            children: {type: 'SectionList', props: {testID: 'emote-grid-list', renderItem: () => ({type: 'Row', props: {onSelectEmote: rowSelect}})}},
            footer: {type: 'ScrollView', props: {testID: 'emote-nav-tablist', children: [
                {type: 'NavTab', key: 'twitchpatches-favorites', props: {category: {key: 'twitchpatches-favorites'}}},
                {type: 'NavTab', key: 'global', props: {category: {key: 'global'}}}
            ]}},
            overlay: {type: 'Menu', props: {children: [{type: 'Heading', props: {}}, insert]}}
        }};
    };
    const cardTree = () => ({type: 'View', props: {children: [
        {type: 'View', props: {style: 'headerRow', children: [{type: 'Image', props: {testID: 'chat-emote-card-image'}}, {type: 'View', props: {style: 'headerText'}}]}},
        {type: 'List', props: {children: [{type: 'Row', props: {testID: 'chat-emote-card-follow'}}, {type: 'Row', props: {testID: 'chat-emote-card-report'}}]}}
    ]}});
    return {
        render: adapters.EmotePickerTray(tree, React, null, modules),
        renderCard: adapters.EmoteCard(cardTree, React, null, modules),
        links, received: () => received, api: runtime.emoteFavorites, selected: () => chosen, rowSelect,
        set: (keys, details = {}) => favorites.set(JSON.stringify({keys, details})), setRaw: value => favorites.set(JSON.stringify(value)),
        setEnabled(value) { enabled = value; }, setPreview(value) { preview = value; }, setCard(value) { card = value; }
    };
}
const sections = [
    {id: 'channel', category: 'current_channel', title: 'Channel', emotes: [twitch('1', 'One'), twitch('2', 'Two', {locked: true})]},
    {id: 'global', category: 'global', title: 'Global', emotes: [twitch('3', 'Three')]}
];
const menu = tree => tree.props.overlay.props.children;
const tokens = section => Array.from(section.emotes, emote => emote.token);
const cardHeader = tree => tree.props.children[0].props.children;

test('favorites keep the saved order and only list emotes available here', () => {
    const f = fixture({catalog: [{emotes: [external('catJAM')]}]});
    f.set(['t:3', 'c:7tv:catJAM', 't:missing', 't:1', 'c:7tv:gone']);
    f.render({channelID: '5', sections});
    const [favorites, ...rest] = f.received().sections;
    assert.equal(favorites.id, 'twitchpatches-favorites');
    assert.equal(favorites.category, 'current_channel');
    assert.equal(favorites.title, 'Favorites');
    assert.deepEqual(tokens(favorites), ['Three', 'catJAM', 'One']);
    assert.equal(rest.length, 2);
    assert.equal(rest[0], sections[0]);
    assert.equal(rest[1], sections[1]);
});

test('a channel emote stays out of a channel where it is not available', () => {
    const f = fixture({catalog: [{emotes: [external('Other')]}]});
    f.set(['c:7tv:catJAM']);
    const props = {channelID: '6', sections};
    f.render(props);
    assert.equal(f.received(), props);
});

test('another provider or emote with the same name does not match', () => {
    const f = fixture({catalog: [{emotes: [{id: 'twitchpatches:cdn.betterttv.net/different', token: 'catJAM'}]}]});
    f.set(['c:7tv:catJAM']);
    const props = {channelID: '6', sections};
    f.render(props);
    assert.equal(f.received(), props);
});

test('an empty list adds no section', () => {
    const f = fixture();
    const props = {channelID: '5', sections};
    f.render(props);
    assert.equal(f.received(), props);
});

test('a locked emote from this picker stays the original locked tile', () => {
    const f = fixture();
    f.set(['t:2']);
    f.render({channelID: '5', sections});
    assert.equal(f.received().sections[0].emotes[0], sections[0].emotes[1]);
});

test('an unlocked copy wins over a locked one', () => {
    const f = fixture();
    f.set(['t:7']);
    const unlocked = twitch('7', 'Seven');
    f.render({channelID: '5', sections: [
        {id: 'a', category: 'global', emotes: [twitch('7', 'Seven', {locked: true})]},
        {id: 'b', category: 'global', emotes: [unlocked]}
    ]});
    assert.equal(f.received().sections[0].emotes[0], unlocked);
});

test('a favorite from another channel shows as a locked tile', () => {
    const f = fixture();
    f.set(['t:99'], {'t:99': {token: 'aurumWave', channel: '123', kind: 'SUBSCRIPTIONS'}});
    f.render({channelID: '5', sections});
    const [emote] = f.received().sections[0].emotes;
    assert.equal(emote.id, '99');
    assert.equal(emote.token, 'aurumWave');
    assert.equal(emote.locked, true);
    assert.equal(emote.unlockKind, 'subscription');
    f.set(['t:98'], {'t:98': {token: 'x', channel: '123', kind: 'FOLLOWER'}});
    f.render({channelID: '5', sections});
    assert.equal(f.received().sections[0].emotes[0].unlockKind, 'follow');
});

test('a favorite with no details and no picker entry is left out', () => {
    const f = fixture();
    f.set(['t:99'], {'t:99': {token: '', channel: 'x', kind: 'MADE_UP'}});
    const props = {channelID: '5', sections};
    f.render(props);
    assert.equal(f.received(), props);
});

test('the setting removes the section and leaves the tree untouched', () => {
    const f = fixture();
    f.set(['t:1']);
    f.setEnabled(false);
    const props = {channelID: '5', sections};
    const tree = f.render(props);
    assert.equal(f.received(), props);
    assert.equal(menu(tree).length, 2);
    assert.equal(tree.props.footer.props.children[0].type, 'NavTab');
});

test('the menu offers to add, toggles through the link and closes', () => {
    let dismissed = 0;
    const f = fixture({preview: {previewEmote: twitch('3', 'Three'), dismissPreview() { dismissed++; }}});
    const tree = f.render({channelID: '5', sections});
    const item = menu(tree)[2];
    assert.equal(item.props.testID, 'emote-preview-favorite');
    assert.equal(item.props.children, 'Add to Favorites');
    assert.equal(item.props.figure.icon, 'IconStarHollow');
    item.props.onPress();
    assert.equal(dismissed, 1);
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=t%3A3']);
    const next = f.render({channelID: '5', sections});
    assert.equal(f.received().sections[0].emotes[0].token, 'Three');
    assert.equal(menu(next)[2].props.children, 'Remove from Favorites');
    assert.equal(menu(next)[2].props.figure.icon, 'IconStar');
});

test('provider emotes toggle by provider and emote id', () => {
    const f = fixture({preview: {previewEmote: external('catJAM'), dismissPreview() {}}});
    menu(f.render({channelID: '5', sections}))[2].props.onPress();
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=c%3A7tv%3AcatJAM']);
});

test('locked emotes can be favorited from the menu with their channel details', () => {
    const f = fixture({preview: {previewEmote: twitch('2', 'Two', {locked: true, unlockKind: 'follow'}), dismissPreview() {}}});
    const item = menu(f.render({channelID: '5', sections}))[2];
    assert.equal(item.props.children, 'Add to Favorites');
    item.props.onPress();
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=t%3A2&token=Two&channel=5&kind=FOLLOWER']);
    f.setPreview(null);
    assert.equal(menu(f.render({channelID: '5', sections})).length, 2);
});

test('removing a favorite drops its details from the link', () => {
    const f = fixture({preview: {previewEmote: twitch('2', 'Two', {locked: true}), dismissPreview() {}}});
    f.set(['t:2'], {'t:2': {token: 'Two', channel: '5', kind: 'SUBSCRIPTIONS'}});
    menu(f.render({channelID: '5', sections}))[2].props.onPress();
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=t%3A2']);
});

test('tapping a locked favorite from elsewhere opens its card, others keep Twitch behavior', () => {
    const f = fixture();
    f.set(['t:99'], {'t:99': {token: 'aurumWave', channel: '123', kind: 'SUBSCRIPTIONS'}});
    const list = f.render({channelID: '5', sections}).props.children;
    const elsewhere = {id: '99', token: 'aurumWave', locked: true, lockedElsewhere: true};
    const row = list.props.renderItem({section: {key: 'twitchpatches-favorites'}, item: {emotes: [elsewhere]}});
    row.props.onSelectEmote(twitch('1', 'One'));
    assert.deepEqual(f.selected(), ['One']);
    row.props.onSelectEmote(elsewhere);
    assert.deepEqual(f.selected(), ['One']);
    const tree = f.render({channelID: '5', sections});
    assert.equal(tree.type, 'Fragment');
    assert.equal(tree.props.children, undefined);
    assert.equal(list.props.renderItem({section: {key: 'channel'}}).props.onSelectEmote, f.rowSelect);
});

test('only the favorites tab is replaced with the star tab', () => {
    const f = fixture();
    const tabs = f.render({channelID: '5', sections}).props.footer.props.children;
    assert.equal(typeof tabs[0].type, 'function');
    assert.equal(tabs[1].type, 'NavTab');
});

test('usable emotes get a header star beside the emote that toggles in place', () => {
    const f = fixture();
    const header = cardHeader(f.renderCard({emote: twitch('3', 'Three')}));
    assert.equal(header.length, 3);
    assert.equal(header[0].props.testID, 'chat-emote-card-image');
    const star = header[2];
    assert.equal(star.props.testID, 'emote-card-favorite');
    assert.equal(star.props.accessibilityLabel, 'Add to Favorites');
    assert.equal(star.props.children.type, 'IconStarHollow');
    assert.equal(star.props.children.props.fillColor, 'text');
    star.props.onPress();
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=t%3A3']);
    const next = cardHeader(f.renderCard({emote: twitch('3', 'Three')}))[2];
    assert.equal(next.props.accessibilityLabel, 'Remove from Favorites');
    assert.equal(next.props.children.type, 'IconStar');
});

test('Twitch rows stay as they are', () => {
    const f = fixture();
    const list = f.renderCard({emote: twitch('3', 'Three')}).props.children[1].props.children;
    assert.deepEqual(Array.from(list, row => row.props.testID), ['chat-emote-card-follow', 'chat-emote-card-report']);
});

test('every Twitch emote gets a star and sends its details for a locked tile', () => {
    const f = fixture();
    const cases = [
        [{emoteType: 'subscription', emoteChannelID: '123', isUserSubscribedToTier: false}, '&token=Nine&channel=123&kind=SUBSCRIPTIONS'],
        [{emoteType: 'follower', emoteChannelID: '123'}, '&token=Nine&channel=123&kind=FOLLOWER'],
        [{emoteType: 'bitsTier', emoteChannelID: '123'}, '&token=Nine&channel=123&kind=BITS_BADGE_TIERS'],
        [{emoteType: 'global'}, '']
    ];
    for (const [card, query] of cases) {
        f.links.length = 0;
        f.set([]);
        f.setCard(card);
        cardHeader(f.renderCard({emote: twitch('9', 'Nine')}))[2].props.onPress();
        assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=t%3A9' + query]);
    }
});

test('the card is untouched when disabled or when it has no emote id', () => {
    const f = fixture();
    assert.equal(cardHeader(f.renderCard({emote: {token: 'catJAM'}})).length, 2);
    f.setEnabled(false);
    assert.equal(cardHeader(f.renderCard({emote: twitch('3', 'Three')})).length, 2);
    assert.equal(cardHeader(f.renderCard({emote: {token: 'catJAM'}, favoriteKey: 'c:7tv:catJAM'})).length, 2);
});

test('provider cards get the same star from their provider key', () => {
    const f = fixture();
    assert.equal(f.api.providerKey('https://cdn.frankerfacez.com/emote/128054/animated/2.gif'), 'c:frankerfacez:128054');
    assert.equal(f.api.providerKey('https://cdn.betterttv.net/emote/abc/2x.webp'), 'c:betterttv:abc');
    assert.equal(f.api.providerKey('https://cdn.example.test/emote/1/2x.webp'), null);
    f.setCard({emoteType: 'unknown'});
    const star = cardHeader(f.renderCard({emote: {token: 'catJAM'}, favoriteKey: 'c:7tv:catJAM'}))[2];
    assert.equal(star.props.testID, 'emote-card-favorite');
    star.props.onPress();
    assert.deepEqual(f.links, ['twitch-patches://favorites/toggle?key=c%3A7tv%3AcatJAM']);
});

test('invalid native data is ignored', () => {
    const f = fixture();
    f.set(['t:1']);
    f.setRaw({not: 'an array'});
    f.setRaw(['t:3']);
    f.render({channelID: '5', sections});
    assert.equal(f.received().sections[0].emotes[0].token, 'One');
    f.set(['bad key', 7, 't:3']);
    f.render({channelID: '5', sections});
    assert.deepEqual(tokens(f.received().sections[0]), ['Three']);
});
