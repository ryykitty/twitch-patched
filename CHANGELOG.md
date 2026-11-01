## [1.5.0](https://github.com/ryykitty/twitch-patched/compare/v1.4.0...v1.5.0) (2026-11-01)

### Bug Fixes

* claim bonus channel points that appear before the stream starts playing ([69cc3fa](https://github.com/ryykitty/twitch-patched/commit/69cc3fa0783bf6102e4717909589ca320c95cedd))
* keep all emote picker tabs visible after tapping a provider in React Native chat ([9352faa](https://github.com/ryykitty/twitch-patched/commit/9352faaa1c3ed13748050159221aa958bbe200bc))
* keep deleted messages readable in native and React Native chat ([6a99df2](https://github.com/ryykitty/twitch-patched/commit/6a99df290c3cf7bc08ed575bcdcc73f0480ee6c0)), closes [#25](https://github.com/ryykitty/twitch-patched/issues/25)
* keep UI settings patching on the latest 31.9.0 beta build ([0551a64](https://github.com/ryykitty/twitch-patched/commit/0551a6457b59437147f575e95201b0387ba166f7))
* let Morphe Manager 2.0.0 and newer load the patches ([cabafa6](https://github.com/ryykitty/twitch-patched/commit/cabafa6b1714686063f83196a07b525451efa9c7))
* list emote suggestions that start with what you typed first ([d8f2897](https://github.com/ryykitty/twitch-patched/commit/d8f2897001dda9a08e64b8e73860448002f3a2cf))
* stop React Native chat crashing as soon as you type ([2aa1045](https://github.com/ryykitty/twitch-patched/commit/2aa1045e6ad7eaa0e9970679b611d7ac94c1c9b3)), closes [#34](https://github.com/ryykitty/twitch-patched/issues/34)
* stop the Live feed jumping when feed ads are hidden ([ce1a9a4](https://github.com/ryykitty/twitch-patched/commit/ce1a9a49b16799158c3fd9b78616610789bd6396)), closes [#25](https://github.com/ryykitty/twitch-patched/issues/25)

### Features

* add favorite emotes to the picker and emote cards ([979d829](https://github.com/ryykitty/twitch-patched/commit/979d829e2ec0642d665f4400cc11bbf45789bd84)), closes [#20](https://github.com/ryykitty/twitch-patched/issues/20)
* show emotes as images in the message box in every chat layout ([83bae3e](https://github.com/ryykitty/twitch-patched/commit/83bae3eba995fee0b244cfc224298681aaa9d7c0))

### App Support

* support only the latest stable and beta Twitch ([c709c33](https://github.com/ryykitty/twitch-patched/commit/c709c33490624f83533019a96c0cac8dc79d594e))

### Improvements

* patch about twice as fast with indexed hook lookups ([133c078](https://github.com/ryykitty/twitch-patched/commit/133c07806cbf90116f5f55a4d8e1d0bc447765e5))
* speed up patching by reusing the React Native bundle analysis ([bc72ec9](https://github.com/ryykitty/twitch-patched/commit/bc72ec94f522925583f4322c1f522d73e7ec342e))

## [1.5.0-dev.2](https://github.com/ryykitty/twitch-patched/compare/v1.5.0-dev.1...v1.5.0-dev.2) (2026-11-01)

### Bug Fixes

* keep all emote picker tabs visible after tapping a provider in React Native chat ([9352faa](https://github.com/ryykitty/twitch-patched/commit/9352faaa1c3ed13748050159221aa958bbe200bc))
* list emote suggestions that start with what you typed first ([d8f2897](https://github.com/ryykitty/twitch-patched/commit/d8f2897001dda9a08e64b8e73860448002f3a2cf))
* stop React Native chat crashing as soon as you type ([2aa1045](https://github.com/ryykitty/twitch-patched/commit/2aa1045e6ad7eaa0e9970679b611d7ac94c1c9b3)), closes [#34](https://github.com/ryykitty/twitch-patched/issues/34)

### Features

* show emotes as images in the message box in every chat layout ([83bae3e](https://github.com/ryykitty/twitch-patched/commit/83bae3eba995fee0b244cfc224298681aaa9d7c0))

### Improvements

* speed up patching by reusing the React Native bundle analysis ([bc72ec9](https://github.com/ryykitty/twitch-patched/commit/bc72ec94f522925583f4322c1f522d73e7ec342e))

## [1.5.0-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.4.0...v1.5.0-dev.1) (2026-11-01)

### Bug Fixes

* claim bonus channel points that appear before the stream starts playing ([69cc3fa](https://github.com/ryykitty/twitch-patched/commit/69cc3fa0783bf6102e4717909589ca320c95cedd))
* keep deleted messages readable in native and React Native chat ([6a99df2](https://github.com/ryykitty/twitch-patched/commit/6a99df290c3cf7bc08ed575bcdcc73f0480ee6c0)), closes [#25](https://github.com/ryykitty/twitch-patched/issues/25)
* keep UI settings patching on the latest 31.9.0 beta build ([0551a64](https://github.com/ryykitty/twitch-patched/commit/0551a6457b59437147f575e95201b0387ba166f7))
* let Morphe Manager 2.0.0 and newer load the patches ([cabafa6](https://github.com/ryykitty/twitch-patched/commit/cabafa6b1714686063f83196a07b525451efa9c7))
* stop the Live feed jumping when feed ads are hidden ([ce1a9a4](https://github.com/ryykitty/twitch-patched/commit/ce1a9a49b16799158c3fd9b78616610789bd6396)), closes [#25](https://github.com/ryykitty/twitch-patched/issues/25)

### Features

* add favorite emotes to the picker and emote cards ([979d829](https://github.com/ryykitty/twitch-patched/commit/979d829e2ec0642d665f4400cc11bbf45789bd84)), closes [#20](https://github.com/ryykitty/twitch-patched/issues/20)

### App Support

* support only the latest stable and beta Twitch ([c709c33](https://github.com/ryykitty/twitch-patched/commit/c709c33490624f83533019a96c0cac8dc79d594e))

### Improvements

* patch about twice as fast with indexed hook lookups ([133c078](https://github.com/ryykitty/twitch-patched/commit/133c07806cbf90116f5f55a4d8e1d0bc447765e5))

## [1.4.0](https://github.com/ryykitty/twitch-patched/compare/v1.3.0...v1.4.0) (2026-10-31)

### Bug Fixes

* hide creator sponsorships in native and React Native players ([e14935a](https://github.com/ryykitty/twitch-patched/commit/e14935ac02abe76262e1f62a2e59744a0b6b65ca))
* keep React Native hooks on the packaged bundle ([9f4857c](https://github.com/ryykitty/twitch-patched/commit/9f4857c49d7e168b4430a954e5d00c9b6b0f43d4))
* preserve background playback preferences on DJ streams ([b63f31d](https://github.com/ryykitty/twitch-patched/commit/b63f31d6326593036d73963a45c5af01a1a997ca))
* restore DJ background playback and update Twitch targets ([7d4bed3](https://github.com/ryykitty/twitch-patched/commit/7d4bed3e89217a5c3f3f77e797a446d0782de078))
* start preroll backup preparation without delay ([2819663](https://github.com/ryykitty/twitch-patched/commit/281966306bcbb7ed8e17aa404b0ac829c2209d86))
* suspend emote draft rendering while Twitch is paused ([ef0128b](https://github.com/ryykitty/twitch-patched/commit/ef0128b314d5c736bc49e24f028bc375e661f550))

### Features

* add external emote autocomplete and draft previews ([4ce1fc7](https://github.com/ryykitty/twitch-patched/commit/4ce1fc74d4ff3ba50171fb00b48cf6f419af19ed))

### App Support

* verify Twitch 31.8.0 and 31.9.0 beta compatibility ([752b79e](https://github.com/ryykitty/twitch-patched/commit/752b79e8ba4e53e512d250b9d3de31aca8a4a162))

## [1.4.0-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.3.0...v1.4.0-dev.1) (2026-10-29)

Twitch 31.8.0 is the primary target; 31.9.0_BETA is experimental. 31.7.0 remains supported.

All ten feature patches passed full APK rebuilding and original-aware SDK checks on each target. 279 Java/Kotlin tests and 62 JavaScript tests passed. This build has not been installed or tested on a phone.

DJ screen-off playback remains unconfirmed. Stream ads can still appear when no clean alternative is available; Stories ad coverage is not verified. See [ad-blocking coverage and research](https://github.com/ryykitty/twitch-patched/blob/v1.4.0-dev.1/docs/AD_BLOCKING.md).

### Bug Fixes

* hide creator sponsorships in native and React Native players ([e14935a](https://github.com/ryykitty/twitch-patched/commit/e14935ac02abe76262e1f62a2e59744a0b6b65ca))
* keep React Native hooks on the packaged bundle ([9f4857c](https://github.com/ryykitty/twitch-patched/commit/9f4857c49d7e168b4430a954e5d00c9b6b0f43d4))
* preserve background playback preferences on DJ streams ([b63f31d](https://github.com/ryykitty/twitch-patched/commit/b63f31d6326593036d73963a45c5af01a1a997ca))
* restore DJ background playback and update Twitch targets ([7d4bed3](https://github.com/ryykitty/twitch-patched/commit/7d4bed3e89217a5c3f3f77e797a446d0782de078))
* start preroll backup preparation without delay ([2819663](https://github.com/ryykitty/twitch-patched/commit/281966306bcbb7ed8e17aa404b0ac829c2209d86))
* suspend emote draft rendering while Twitch is paused ([ef0128b](https://github.com/ryykitty/twitch-patched/commit/ef0128b314d5c736bc49e24f028bc375e661f550))

### Features

* add external emote autocomplete and draft previews ([4ce1fc7](https://github.com/ryykitty/twitch-patched/commit/4ce1fc74d4ff3ba50171fb00b48cf6f419af19ed))

### App Support

* verify Twitch 31.8.0 and 31.9.0 beta compatibility ([752b79e](https://github.com/ryykitty/twitch-patched/commit/752b79e8ba4e53e512d250b9d3de31aca8a4a162))

## [1.3.0](https://github.com/ryykitty/twitch-patched/compare/v1.2.0...v1.3.0) (2026-10-26)

### Bug Fixes

* hide remaining channel actions and collapse empty player rows ([3b11aec](https://github.com/ryykitty/twitch-patched/commit/3b11aec84a845e4e2cff8d6fcf6b86ee98c5b1b4))
* match emote asset instructions with Morphe Patcher 2 ([3513569](https://github.com/ryykitty/twitch-patched/commit/351356920df6aca04dc029b6f2973056b2039ed0))
* refresh reload bindings when native player controls are reused ([2b21abf](https://github.com/ryykitty/twitch-patched/commit/2b21abf09342231ebc93dc191571845cf0a3b018))
* remove vertical feed ad slots and reserved banner spacing ([9f90570](https://github.com/ryykitty/twitch-patched/commit/9f905703c0a291370182ba98f12412140a0d9d36))
* reveal retained deleted messages in native and React Native chat ([776e6ea](https://github.com/ryykitty/twitch-patched/commit/776e6ea0f2be295e171fc723d860aade8a06023e))
* simplify patch settings rows and show group visibility summaries ([ae63ea8](https://github.com/ryykitty/twitch-patched/commit/ae63ea8fa55f743b7eeeafa060528f6660f84a2c))
* stabilize fullscreen system bars and live settings changes ([7492cb5](https://github.com/ryykitty/twitch-patched/commit/7492cb5c416d1264ede039c5b8287697cf66238f))
* support Twitch 31.7.0 ([d3dd457](https://github.com/ryykitty/twitch-patched/commit/d3dd45761438b8358c6e27c246972ddab26a21fd))

### Features

* add Hype Train and leaderboard visibility settings ([863b7df](https://github.com/ryykitty/twitch-patched/commit/863b7dfe0cbe965144ab750ccffb318c88de50a6))
* show deleted messages ([e31bd3a](https://github.com/ryykitty/twitch-patched/commit/e31bd3aa73b206bf4524057147934a056837d860))

### App Support

* support Twitch 31.8 beta and Morphe Patcher 2 ([48359d9](https://github.com/ryykitty/twitch-patched/commit/48359d993273adf04029e1bfed24ed2bef9e9f8f))

## [1.3.0-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.2.0...v1.3.0-dev.1) (2026-10-26)

### Bug Fixes

* hide remaining channel actions and collapse empty player rows ([3b11aec](https://github.com/ryykitty/twitch-patched/commit/3b11aec84a845e4e2cff8d6fcf6b86ee98c5b1b4))
* match emote asset instructions with Morphe Patcher 2 ([3513569](https://github.com/ryykitty/twitch-patched/commit/351356920df6aca04dc029b6f2973056b2039ed0))
* refresh reload bindings when native player controls are reused ([2b21abf](https://github.com/ryykitty/twitch-patched/commit/2b21abf09342231ebc93dc191571845cf0a3b018))
* remove vertical feed ad slots and reserved banner spacing ([9f90570](https://github.com/ryykitty/twitch-patched/commit/9f905703c0a291370182ba98f12412140a0d9d36))
* reveal retained deleted messages in native and React Native chat ([776e6ea](https://github.com/ryykitty/twitch-patched/commit/776e6ea0f2be295e171fc723d860aade8a06023e))
* simplify patch settings rows and show group visibility summaries ([ae63ea8](https://github.com/ryykitty/twitch-patched/commit/ae63ea8fa55f743b7eeeafa060528f6660f84a2c))
* stabilize fullscreen system bars and live settings changes ([7492cb5](https://github.com/ryykitty/twitch-patched/commit/7492cb5c416d1264ede039c5b8287697cf66238f))
* support Twitch 31.7.0 ([d3dd457](https://github.com/ryykitty/twitch-patched/commit/d3dd45761438b8358c6e27c246972ddab26a21fd))

### Features

* add Hype Train and leaderboard visibility settings ([863b7df](https://github.com/ryykitty/twitch-patched/commit/863b7dfe0cbe965144ab750ccffb318c88de50a6))
* show deleted messages ([e31bd3a](https://github.com/ryykitty/twitch-patched/commit/e31bd3aa73b206bf4524057147934a056837d860))

### App Support

* support Twitch 31.8 beta and Morphe Patcher 2 ([48359d9](https://github.com/ryykitty/twitch-patched/commit/48359d993273adf04029e1bfed24ed2bef9e9f8f))

## [1.2.0](https://github.com/ryykitty/twitch-patched/compare/v1.1.0...v1.2.0) (2026-10-17)

### Bug Fixes

* cover native vertical player settings and layouts ([babdce8](https://github.com/ryykitty/twitch-patched/commit/babdce8b8e60e7544f84909586a8aa3c758804b4))
* reduce UI settings patch memory usage ([993645d](https://github.com/ryykitty/twitch-patched/commit/993645d623c8bebb035a17d0d73ab8b51f7e668b))
* refresh fullscreen cutout padding when settings change ([acc8d13](https://github.com/ryykitty/twitch-patched/commit/acc8d13b536e38d81fd761b883b6c1a7a5c6ed2b))
* refresh system bars in unlocked fullscreen playback ([f7dd48a](https://github.com/ryykitty/twitch-patched/commit/f7dd48ac0fd0fb6fd1a8395d537b9c2e273acb3e))
* remove reserved cutout padding in fullscreen ([1c0670d](https://github.com/ryykitty/twitch-patched/commit/1c0670d4cf297ccd7d21eebac57b0066b4487c75))
* render external emotes in replay and offline chat ([ce357ef](https://github.com/ryykitty/twitch-patched/commit/ce357ef0200db340e4acbb22c67ae7f5ecc75b1d))
* restore settings window insets when system bars are shown ([6737767](https://github.com/ryykitty/twitch-patched/commit/6737767ab8fe80ef7ffbed54abca2a44c48445c5))
* stabilize channel actions and restore replay chat ([84bafa6](https://github.com/ryykitty/twitch-patched/commit/84bafa63c88413f49cf629f146da31818dd2c390))

### Features

* add channel extension and action row visibility settings ([1056063](https://github.com/ryykitty/twitch-patched/commit/1056063b4906aaec6339000df2b672ae02edc247))
* add grouped player visibility settings ([85f8e32](https://github.com/ryykitty/twitch-patched/commit/85f8e32cf7fab32d205846966fa9ce6aaf1f3c89))
* add searchable categorized patch settings ([9434872](https://github.com/ryykitty/twitch-patched/commit/943487213bf20e9657630c45bc33e445dadf5ef1))
* add UI settings foundation ([b70482e](https://github.com/ryykitty/twitch-patched/commit/b70482e5042d5b0e8de42d7f944b6f5660889aed))
* add vertical header controls and V2 settings links ([7578b05](https://github.com/ryykitty/twitch-patched/commit/7578b055721fbd436db0b333e4e19f20f0420f12))
* organize UI settings by control ([10fe366](https://github.com/ryykitty/twitch-patched/commit/10fe3667f4ca56816322d158dd26546a9bf9c011))

### App Support

* support Twitch 31.6.0 ([d8df17d](https://github.com/ryykitty/twitch-patched/commit/d8df17d286a12d516aece9b82b8e30c70a4669a1))

### Improvements

* reduce repeated UI hook searches ([f372baf](https://github.com/ryykitty/twitch-patched/commit/f372baf769da9065b2f073303d46f5b3847375f0))

## [1.2.0-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.1.0...v1.2.0-dev.1) (2026-10-16)

### Bug Fixes

* cover native vertical player settings and layouts ([babdce8](https://github.com/ryykitty/twitch-patched/commit/babdce8b8e60e7544f84909586a8aa3c758804b4))
* reduce UI settings patch memory usage ([993645d](https://github.com/ryykitty/twitch-patched/commit/993645d623c8bebb035a17d0d73ab8b51f7e668b))
* refresh fullscreen cutout padding when settings change ([acc8d13](https://github.com/ryykitty/twitch-patched/commit/acc8d13b536e38d81fd761b883b6c1a7a5c6ed2b))
* refresh system bars in unlocked fullscreen playback ([f7dd48a](https://github.com/ryykitty/twitch-patched/commit/f7dd48ac0fd0fb6fd1a8395d537b9c2e273acb3e))
* remove reserved cutout padding in fullscreen ([1c0670d](https://github.com/ryykitty/twitch-patched/commit/1c0670d4cf297ccd7d21eebac57b0066b4487c75))
* render external emotes in replay and offline chat ([ce357ef](https://github.com/ryykitty/twitch-patched/commit/ce357ef0200db340e4acbb22c67ae7f5ecc75b1d))
* restore settings window insets when system bars are shown ([6737767](https://github.com/ryykitty/twitch-patched/commit/6737767ab8fe80ef7ffbed54abca2a44c48445c5))
* stabilize channel actions and restore replay chat ([84bafa6](https://github.com/ryykitty/twitch-patched/commit/84bafa63c88413f49cf629f146da31818dd2c390))

### Features

* add channel extension and action row visibility settings ([1056063](https://github.com/ryykitty/twitch-patched/commit/1056063b4906aaec6339000df2b672ae02edc247))
* add grouped player visibility settings ([85f8e32](https://github.com/ryykitty/twitch-patched/commit/85f8e32cf7fab32d205846966fa9ce6aaf1f3c89))
* add searchable categorized patch settings ([9434872](https://github.com/ryykitty/twitch-patched/commit/943487213bf20e9657630c45bc33e445dadf5ef1))
* add UI settings foundation ([b70482e](https://github.com/ryykitty/twitch-patched/commit/b70482e5042d5b0e8de42d7f944b6f5660889aed))
* add vertical header controls and V2 settings links ([7578b05](https://github.com/ryykitty/twitch-patched/commit/7578b055721fbd436db0b333e4e19f20f0420f12))
* organize UI settings by control ([10fe366](https://github.com/ryykitty/twitch-patched/commit/10fe3667f4ca56816322d158dd26546a9bf9c011))

### App Support

* support Twitch 31.6.0 ([d8df17d](https://github.com/ryykitty/twitch-patched/commit/d8df17d286a12d516aece9b82b8e30c70a4669a1))

### Improvements

* reduce repeated UI hook searches ([f372baf](https://github.com/ryykitty/twitch-patched/commit/f372baf769da9065b2f073303d46f5b3847375f0))

## [1.1.0](https://github.com/ryykitty/twitch-patched/compare/v1.0.4...v1.1.0) (2026-10-09)

### Bug Fixes

* block Following and V2 display ads ([781a761](https://github.com/ryykitty/twitch-patched/commit/781a7618fc7366a7ddefb4791148f97ed44715e5))
* hide native Turbo startup offers ([a37999c](https://github.com/ryykitty/twitch-patched/commit/a37999c121a86229d1acdd4ec7b2af419cc7930a))
* include shared fixes with playback diagnostics ([f16cd6b](https://github.com/ryykitty/twitch-patched/commit/f16cd6b13536bc392209a2f2704f72e6db950a81))
* preserve feed themes after loading and navigation ([72b2531](https://github.com/ryykitty/twitch-patched/commit/72b2531b05c6e19cd89488004f1ea4d91264da78))
* restore DJ stream playback settings ([fdfa7db](https://github.com/ryykitty/twitch-patched/commit/fdfa7dbc6ee4b10b110c99ecef920d2447824226))
* synchronize feed and settings themes ([85ff0fd](https://github.com/ryykitty/twitch-patched/commit/85ff0fd30c806ebb9db8a5f98c51e22ff5a00855))
* synchronize patch and settings descriptions ([09cc4b2](https://github.com/ryykitty/twitch-patched/commit/09cc4b2a8327e4a11b8fadb830f03f23a3c1cd9d))

### Features

* add emote provider settings ([0834660](https://github.com/ryykitty/twitch-patched/commit/0834660d6a4921567323ff4ac4be9b6898a8e2b2))
* add external emote catalogs to native and V2 pickers ([a5c1acd](https://github.com/ryykitty/twitch-patched/commit/a5c1acd14cc9aed043dfd3d2cfc619bb839b9b83))
* add FrankerFaceZ and fix animated emotes ([a5ecb0d](https://github.com/ryykitty/twitch-patched/commit/a5ecb0dc1c0b2bc50339f53316062a523272d8b5))

### App Support

* support Twitch 31.5.2 ([fb68452](https://github.com/ryykitty/twitch-patched/commit/fb68452d1f0887371fa4500453fc6d37cdabc0ea))

## [1.1.0-dev.3](https://github.com/ryykitty/twitch-patched/compare/v1.1.0-dev.2...v1.1.0-dev.3) (2026-10-09)

### Bug Fixes

* synchronize patch and settings descriptions ([09cc4b2](https://github.com/ryykitty/twitch-patched/commit/09cc4b2a8327e4a11b8fadb830f03f23a3c1cd9d))

## [1.1.0-dev.2](https://github.com/ryykitty/twitch-patched/compare/v1.1.0-dev.1...v1.1.0-dev.2) (2026-10-09)

### Features

* add external emote catalogs to native and V2 pickers ([a5c1acd](https://github.com/ryykitty/twitch-patched/commit/a5c1acd14cc9aed043dfd3d2cfc619bb839b9b83))

## [1.1.0-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.0.4...v1.1.0-dev.1) (2026-10-08)

### Bug Fixes

* block Following and V2 display ads ([781a761](https://github.com/ryykitty/twitch-patched/commit/781a7618fc7366a7ddefb4791148f97ed44715e5))
* hide native Turbo startup offers ([a37999c](https://github.com/ryykitty/twitch-patched/commit/a37999c121a86229d1acdd4ec7b2af419cc7930a))
* include shared fixes with playback diagnostics ([f16cd6b](https://github.com/ryykitty/twitch-patched/commit/f16cd6b13536bc392209a2f2704f72e6db950a81))
* preserve feed themes after loading and navigation ([72b2531](https://github.com/ryykitty/twitch-patched/commit/72b2531b05c6e19cd89488004f1ea4d91264da78))
* restore DJ stream playback settings ([fdfa7db](https://github.com/ryykitty/twitch-patched/commit/fdfa7dbc6ee4b10b110c99ecef920d2447824226))
* synchronize feed and settings themes ([85ff0fd](https://github.com/ryykitty/twitch-patched/commit/85ff0fd30c806ebb9db8a5f98c51e22ff5a00855))

### Features

* add emote provider settings ([0834660](https://github.com/ryykitty/twitch-patched/commit/0834660d6a4921567323ff4ac4be9b6898a8e2b2))
* add FrankerFaceZ and fix animated emotes ([a5ecb0d](https://github.com/ryykitty/twitch-patched/commit/a5ecb0dc1c0b2bc50339f53316062a523272d8b5))

### App Support

* support Twitch 31.5.2 ([fb68452](https://github.com/ryykitty/twitch-patched/commit/fb68452d1f0887371fa4500453fc6d37cdabc0ea))

## [1.0.4](https://github.com/ryykitty/twitch-patched/compare/v1.0.3...v1.0.4) (2026-10-04)

### Bug Fixes

* display Twitch 31.4.2 in the community directory ([bc6dd9f](https://github.com/ryykitty/twitch-patched/commit/bc6dd9f64c5964c1ee9c7373a836cc339bcc353a))
* select and enable ad blockers by default ([b371d8b](https://github.com/ryykitty/twitch-patched/commit/b371d8bf85ff518e6d52ae7458de7b05235829df))

## [1.0.3](https://github.com/ryykitty/twitch-patched/compare/v1.0.2...v1.0.3) (2026-10-04)

### Bug Fixes

* recognize tested Twitch versions as supported ([d0f67f9](https://github.com/ryykitty/twitch-patched/commit/d0f67f9cf8c922904bfda9aaa292506a01426701))
* support XML resource parsing in Morphe Manager ([bcb4fbd](https://github.com/ryykitty/twitch-patched/commit/bcb4fbd4836675c643fcda26a59bcbdd029946c4))

## [1.0.3-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.0.2...v1.0.3-dev.1) (2026-10-04)

### Bug Fixes

* recognize tested Twitch versions as supported ([d0f67f9](https://github.com/ryykitty/twitch-patched/commit/d0f67f9cf8c922904bfda9aaa292506a01426701))
* support XML resource parsing in Morphe Manager ([bcb4fbd](https://github.com/ryykitty/twitch-patched/commit/bcb4fbd4836675c643fcda26a59bcbdd029946c4))

## [1.0.2](https://github.com/ryykitty/twitch-patched/compare/v1.0.1...v1.0.2) (2026-10-04)

### Bug Fixes

* **build:** preserve Android patch bundles during verification ([ea2bbe8](https://github.com/ryykitty/twitch-patched/commit/ea2bbe8a0ffcf63acb3cb1430f10b3252e0f6eb0))
* **build:** select the current bundle for release metadata ([03ea818](https://github.com/ryykitty/twitch-patched/commit/03ea8188328b4ee65a23cc8f388047c5bcabf5f4))
* clarify patch descriptions ([e1971f0](https://github.com/ryykitty/twitch-patched/commit/e1971f0383e88542debb103893fd4a57b8c40119))
* prevent overlapping release publication ([d110ebe](https://github.com/ryykitty/twitch-patched/commit/d110ebe87ac2184b344e02821254997a04b1e64f))
* update settings, chat and ad hooks for Twitch 31.4.2 ([a9b9638](https://github.com/ryykitty/twitch-patched/commit/a9b9638344434884d4c0f7193b5be0f2b66b35d7))

### App Support

* add Twitch 31.4.2 compatibility ([db8637b](https://github.com/ryykitty/twitch-patched/commit/db8637bc938ad0d3a25c784b6dc6723128300928))

## [1.0.2-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.0.1...v1.0.2-dev.1) (2026-10-04)

### Bug Fixes

* **build:** preserve Android patch bundles during verification ([ea2bbe8](https://github.com/ryykitty/twitch-patched/commit/ea2bbe8a0ffcf63acb3cb1430f10b3252e0f6eb0))
* **build:** select the current bundle for release metadata ([03ea818](https://github.com/ryykitty/twitch-patched/commit/03ea8188328b4ee65a23cc8f388047c5bcabf5f4))
* clarify patch descriptions ([e1971f0](https://github.com/ryykitty/twitch-patched/commit/e1971f0383e88542debb103893fd4a57b8c40119))
* prevent overlapping release publication ([d110ebe](https://github.com/ryykitty/twitch-patched/commit/d110ebe87ac2184b344e02821254997a04b1e64f))
* update settings, chat and ad hooks for Twitch 31.4.2 ([a9b9638](https://github.com/ryykitty/twitch-patched/commit/a9b9638344434884d4c0f7193b5be0f2b66b35d7))

### App Support

* add Twitch 31.4.2 compatibility ([db8637b](https://github.com/ryykitty/twitch-patched/commit/db8637bc938ad0d3a25c784b6dc6723128300928))

## [1.0.1](https://github.com/ryykitty/twitch-patched/compare/v1.0.0...v1.0.1) (2026-10-04)

### Bug Fixes

* restore Firebase push registration after patching ([b72dbef](https://github.com/ryykitty/twitch-patched/commit/b72dbef984f2239642be5897cdbf908580cca324))

## [1.0.1-dev.1](https://github.com/ryykitty/twitch-patched/compare/v1.0.0...v1.0.1-dev.1) (2026-10-03)

### Bug Fixes

* restore Firebase push registration after patching ([b72dbef](https://github.com/ryykitty/twitch-patched/commit/b72dbef984f2239642be5897cdbf908580cca324))

## 1.0.0 (2026-10-03)

### Features

- **Block stream ads:** replace detected live-stream ads with direct Twitch playback from alternate player contexts, preferring matching quality without an external stream proxy.
- **Hide feed and display ads:** remove sponsored feed cards and display advertisements.
- **Hide Turbo promotions:** remove Turbo entries, upsells and purchase buttons while retaining train overlays.
- **Hide subscription discount banners:** remove subscription offers and promotional labels while retaining normal subscription actions.
- **Auto-claim bonus channel points:** claim available bonus rewards during live playback.
- **BTTV and 7TV emotes:** render global and channel emotes in chat and show provider previews on tap.
- **Reload stream:** reload live streams with a double-tap control.
- **Block client-requested ads:** optionally suppress native ad requests.
- **Playback diagnostics:** provide playlist and playback diagnostics, disabled by default.
- **Inspect Twitch APK:** report package, version and DEX information without modifying the APK.

### Integration

- Add a Patch settings page for selected features.
- Support native and swipe-feed playback and chat.
- Include build, test and APK verification tools.

### Compatibility

- Evaluated target: Twitch **31.3.0 (3103006)**, ARM64, Android 13.
- Extended ad coverage remains under evaluation. An unmatched-media midroll case remains under investigation; story ads are not covered.
- Google Play billing is unavailable in the re-signed app. Other versions and ABIs require separate verification.
