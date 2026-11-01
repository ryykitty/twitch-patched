const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const {test} = require('node:test');

const settle = () => new Promise(resolve => setImmediate(resolve));
const catalogs = {
    'https://flxrs.com/api/badges': [{url: 'https://api.dank.chat/badges/dank.png', users: ['20', '40']}],
    'https://api.chatterino.com/badges': {badges: [{image3: 'https://fourtf.com/chatterino/badges/dev3x.png', users: ['30', '40', '50']}]},
    'https://api.frankerfacez.com/v1/badges/ids': {badges: [
        {id: 3, color: '#755000', urls: {'2': 'https://cdn.frankerfacez.com/badge/3/2'}},
        {id: 1, color: '#FAAF19', urls: {'2': 'https://cdn.frankerfacez.com/badge/1/2'}}],
    users: {'3': [50, 60, 61], '1': [61]}}
};

function fixture() {
    const decorated = new Map(), requests = [], policies = Array(19).fill(false), timers = [];
    policies[15] = policies[16] = policies[17] = policies[18] = true;
    const runtime = {
        decorateType(id, name, adapter) { decorated.set(name, adapter); },
        decorate(id, name, adapter) { decorated.set(name, adapter); },
        enabled(index) { return policies[index]; }, subscribe() { return () => {}; }, log() {}
    };
    const lookupResults = {};
    const context = {__twitchPatchRuntime: runtime, Map, Set, Date, Object, Array, Math, JSON, String, Promise,
        setTimeout(callback) { timers.push(callback); return timers.length; },
        fetch(url, options) {
            requests.push({url, options});
            if (url === 'https://7tv.io/v4/gql') {
                return Promise.resolve({ok: true, json: () => Promise.resolve({data: {users: lookupResults}})});
            }
            return Promise.resolve({ok: true, text: () => Promise.resolve(JSON.stringify(catalogs[url]))});
        }};
    vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../patches/src/main/resources/reactnative/badges.js'), 'utf8')
        .replace(/__TWITCH_[A-Z_]+_MODULE__/g, '1'), context);
    const effects = [];
    const element = (type, props, ...children) => ({type, props: Object.assign({}, props, children.length ? {children: children[0]} : {}), element: true});
    const React = {
        createElement: element,
        useSyncExternalStore(subscribe, snapshot) { return snapshot(); },
        useEffect(callback) { effects.push(callback); },
        useMemo(callback) { return callback(); },
        isValidElement: value => Boolean(value && value.element),
        cloneElement: (node, props, children) => element(node.type, Object.assign({}, node.props, props), ...(children === undefined ? [] : [children])),
        Children: {map: (children, callback) => (Array.isArray(children) ? children : [children]).map(callback)}
    };
    let received;
    const Row = decorated.get('ChatMessageRow')(function (props) { received = props; return 'row'; }, React);
    const render = (userId, badges = {subscriber: '12'}) => {
        effects.length = 0;
        Row({message: {userId, badges}, badgeRegistry: {'subscriber/12': {image1x: 'twitch'}}});
        effects.forEach(effect => effect());
        return received;
    };
    return {render, requests, policies, timers, lookupResults, React, element,
        Part: adapter => decorated.get('BadgePart')(adapter, React)};
}

async function loaded(f) {
    f.render('999');
    await settle();
    await settle();
}

test('a badge is added after the Twitch badges and resolves through the registry', async () => {
    const f = fixture();
    await loaded(f);
    const props = f.render('30');
    assert.deepEqual(Object.keys(props.message.badges), ['subscriber', 'twitchpatches-chatterino']);
    const key = 'twitchpatches-chatterino/' + props.message.badges['twitchpatches-chatterino'];
    assert.equal(props.badgeRegistry[key].image2x, 'https://fourtf.com/chatterino/badges/dev3x.png');
    assert.equal(props.badgeRegistry['subscriber/12'].image1x, 'twitch', 'Twitch badges keep resolving');
});

test('someone with several badges shows the first enabled provider', async () => {
    const f = fixture();
    await loaded(f);
    assert.ok(f.render('40').message.badges['twitchpatches-dankchat'], 'DankChat comes before Chatterino');
    assert.ok(f.render('50').message.badges['twitchpatches-chatterino'], 'Chatterino comes before FrankerFaceZ');
    f.policies[16] = false;
    assert.ok(f.render('40').message.badges['twitchpatches-chatterino'], 'turning DankChat off reveals the next badge');
    f.policies[17] = false;
    assert.ok(f.render('50').message.badges['twitchpatches-ffz']);
});

test('frankerfacez users keep their rarest badge and turning every provider off changes nothing', async () => {
    const f = fixture();
    await loaded(f);
    const props = f.render('61');
    const key = 'twitchpatches-ffz/' + props.message.badges['twitchpatches-ffz'];
    assert.equal(props.badgeRegistry[key].image2x, 'https://cdn.frankerfacez.com/badge/1/2');
    assert.equal(props.badgeRegistry[key].background, '#FAAF19');
    f.policies[15] = f.policies[16] = f.policies[17] = f.policies[18] = false;
    const off = f.render('61');
    assert.deepEqual(Object.keys(off.message.badges), ['subscriber']);
});

test('7TV lookups are batched and its badge comes first', async () => {
    const f = fixture();
    await loaded(f);
    f.render('50');
    f.render('70');
    assert.equal(f.timers.length, 1, 'one batch is scheduled at a time');
    f.lookupResults.u50 = {style: {activeBadge: {images: [
        {url: 'https://cdn.7tv.app/badge/a/2x.webp', mime: 'image/webp', scale: 2, frameCount: 90},
        {url: 'https://cdn.7tv.app/badge/a/2x_static.webp', mime: 'image/webp', scale: 2, frameCount: 1}]}}};
    f.lookupResults.u70 = null;
    f.timers.shift()();
    await settle();
    await settle();
    const lookup = f.requests.filter(request => request.url === 'https://7tv.io/v4/gql');
    assert.equal(lookup.length, 1);
    assert.match(JSON.parse(lookup[0].options.body).query, /u50: userByConnection.*u70: userByConnection/s);
    const props = f.render('50');
    assert.equal(props.message.badges['twitchpatches-chatterino'], undefined);
    const key = 'twitchpatches-7tv/' + props.message.badges['twitchpatches-7tv'];
    assert.equal(props.badgeRegistry[key].image2x, 'https://cdn.7tv.app/badge/a/2x_static.webp');
    assert.deepEqual(Object.keys(f.render('70').message.badges), ['subscriber']);
    assert.equal(f.timers.length, 0, 'known users are not looked up again');
});

test('frankerfacez badges get their colour behind the white image', () => {
    const f = fixture();
    const tree = f.element('Text', {testID: 'chat-badges', children: [
        f.element('View', {testID: 'chat-badge-subscriber', style: 'a'}),
        f.element('View', {testID: 'chat-badge-twitchpatches-ffz', style: 'b'}, 'image')]});
    const Part = f.Part(() => tree);
    const plain = Part({badges: {subscriber: '12'}, registry: {}});
    assert.equal(plain, tree);
    const tinted = Part({badges: {'twitchpatches-ffz': '1'}, registry: {'twitchpatches-ffz/1': {background: '#755000'}}});
    assert.equal(tinted.props.children[0].props.style, 'a');
    const wrapper = tinted.props.children[1];
    assert.equal(wrapper.props.style, 'b', "Twitch's wrapper keeps its spacing");
    assert.equal(JSON.stringify(wrapper.props.children.props.style), '{"alignSelf":"flex-start","backgroundColor":"#755000","borderRadius":3}');
    assert.equal(wrapper.props.children.props.children, 'image', 'the colour sits directly around the image');
});

test('images on other hosts are ignored', async () => {
    const original = catalogs['https://flxrs.com/api/badges'];
    catalogs['https://flxrs.com/api/badges'] = [{url: 'https://example.test/a.png', users: ['20']}];
    const f = fixture();
    await loaded(f);
    assert.deepEqual(Object.keys(f.render('20').message.badges), ['subscriber']);
    catalogs['https://flxrs.com/api/badges'] = original;
});
