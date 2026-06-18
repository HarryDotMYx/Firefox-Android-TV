# Search Engines

The browser ships with many search engines and lets a chosen engine drive the
URL bar.

## What's bundled

OpenSearch plugin XML files live in
[`app/src/main/assets/searchplugins/`](../../app/src/main/assets/searchplugins/).
In addition to the locale defaults, these worldwide engines are always loaded:

Google · Bing · DuckDuckGo · Yahoo · Yandex · Baidu · Ecosia · Brave ·
Startpage · Wikipedia · YouTube

## How it works

1. **`SearchEngineProviderWrapper`** wraps Mozilla's
   `AssetsSearchEngineProvider`. It passes an `additionalIdentifiers` list so the
   worldwide engines load regardless of the device locale, and it can also
   *replace* an engine (e.g. swap Google for the Amazon/Fire TV Google variant).
2. **`SearchEngineManagerFactory`** registers the worldwide engine identifiers
   and creates the `SearchEngineManager` (stored in the `ServiceLocator`).
3. **`UrlUtils.createSearchUrl(context, term)`** picks the engine: if the user
   has selected one (`Settings.defaultSearchEngineName`, key
   `pref_search_engine`), it is used; otherwise the locale default is used.

```kotlin
val url = UrlUtils.createSearchUrl(context, "hello world")
// -> https://www.google.com/search?q=hello+world  (or the selected engine)
```

## Adding a new engine

1. Create an OpenSearch plugin at
   `app/src/main/assets/searchplugins/<id>.xml`. Mirror an existing file; give it
   a **unique `<ShortName>`** and a `text/html` `Url` template containing
   `{searchTerms}`:

   ```xml
   <SearchPlugin xmlns="http://www.mozilla.org/2006/browser/search/">
     <ShortName>Example</ShortName>
     <InputEncoding>UTF-8</InputEncoding>
     <Image width="16" height="16">data:image/png;base64,…</Image>
     <Url type="text/html" method="GET" template="https://example.com/search?q={searchTerms}"/>
     <SearchForm>https://example.com</SearchForm>
   </SearchPlugin>
   ```

2. Add the file name (without `.xml`) to `worldwideEngineIdentifiers` in
   `search/SearchEngineManagerFactory.kt` so it always loads.

3. Build & run; the engine becomes available to the `SearchEngineManager`.

> Tip: keep identifiers distinct from Mozilla's bundled ids (this fork suffixes
> them with `-intl`) to avoid collisions during loading.

## Notes & limitations

- Engine **suggestions** are included for engines that publish a simple
  suggestion endpoint.
- There is currently **no in-app picker UI** for choosing the engine on the TV
  settings screen — the preference plumbing exists, but a leanback picker screen
  is a separate task that needs on-device focus testing.
