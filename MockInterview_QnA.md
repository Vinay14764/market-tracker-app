# Mock Interview — MarketTracker (4 YOE Android Developer)

Questions tied directly to your codebase. Answers are written the way a strong 4-YOE candidate should deliver them.

---

## Round 1 — Architecture & Clean Architecture

**Q1. Walk me through your project in 2 minutes.**
MarketTracker is a crypto market app that fetches data from the CoinGecko API, caches it in Room, and shows a market list, coin detail with sparkline, top gainers/losers, a portfolio screen, login, and a dashboard. It's built with Jetpack Compose for UI, Hilt for DI, Retrofit for networking, Room for local persistence, and Kotlin Coroutines + Flow for async work. The architecture is Clean Architecture (data → domain → ui) with MVI on the screens.

**Q2. Why a domain layer with use cases instead of injecting the repository into the ViewModel directly?**
Three reasons. (1) **Single Responsibility** — each use case names exactly one business action (`ToggleFavoriteUseCase`, `RefreshCoinsUseCase`), so the ViewModel reads like a script of intents. (2) **Reusability** — `GetTopGainersUseCase` and `GetTopLosersUseCase` both reuse `getCoins()` but apply different sorting; that logic lives in one place, not duplicated across ViewModels. (3) **Testability** — I can mock a use case with a one-line lambda; mocking a whole repository pulls in DAO + API. The trade-off is more files, which is fine for an app this size.

**Q3. Why is `CoinRepositoryImpl` `@Singleton`? Would anything actually break without it?**
The strict reason is consistency: Room's DAO is already a singleton-backed flow source, and the API client is a singleton, so two repository instances wouldn't cause data inconsistency *today*. But once I add **in-memory caching, request deduplication, or pagination state** inside the repo (which is a normal next step), two instances would hold separate caches and fight each other. So `@Singleton` is a forward-looking guard, not strictly required by current code.

---

## Round 2 — Kotlin Coroutines & Flow

**Q4. Walk me through `observeFilteredCoins()` in `MarketViewModel`. Why `combine` of three flows?**
`combine` lets me derive the visible list from three independent inputs — the live Room flow (`getCoins()`), the debounced search query, and the active filter tab. Whenever any of them emits, the lambda re-runs and produces a new filtered list, which I push into `_state`. This is reactive: if the user toggles a favorite, Room re-emits, `combine` re-runs, the UI redraws — no manual refresh needed.

**Q5. You debounce the search inside `onIntent` (300ms delay job) instead of using `.debounce(300)` on the flow. Why?**
Because I need the TextField to update **instantly** (good UX) but I only want to **filter** after the user stops typing. So `_state.searchQuery` is updated synchronously on every keystroke (drives the TextField), and `_searchQuery` (the flow used in `combine`) is only updated after 300ms of silence. Putting `.debounce(300)` on a single flow would either delay the TextField redraw or require splitting flows anyway. I cancel the previous debounce job on each keystroke so only the last one survives.

**Q6. Why is `effect` a `Channel` and `state` a `StateFlow`? Why not use the same primitive for both?**
They serve different semantics. **State is a value** — the latest one is always correct, replays are fine, conflation is fine; that's exactly `StateFlow`. **Effects are events** — "navigate", "show snackbar"; if the screen rotates, I do not want to re-navigate or re-show. `Channel(BUFFERED)` delivers each event exactly once to a single collector. Using `SharedFlow` with replay=0 is a valid alternative; `StateFlow` for events is wrong because it conflates and replays.

**Q7. What's the lifecycle of `viewModelScope` and what happens to in-flight network calls when the user backs out of the screen?**
`viewModelScope` is tied to `ViewModel.onCleared()`, which fires when the ViewModel's owner is permanently destroyed (not on configuration change). When that happens, all child coroutines are cancelled cooperatively. Retrofit + OkHttp respect cancellation — the underlying HTTP call is aborted. So a user backing out of a screen mid-refresh won't leak a request or crash on a stale state update.

---

## Round 3 — Jetpack Compose

**Q8. How does the Compose UI know to redraw when your StateFlow emits?**
In the Composable I do `val state by viewModel.state.collectAsStateWithLifecycle()` (or `collectAsState`). That returns a `State<T>`, and Compose tracks reads of `State` objects during composition. When the value changes, Compose invalidates only the composables that read it and re-runs them — that's recomposition. I'm not redrawing the whole screen, only the parts that read the changed slice.

**Q9. What's the difference between `remember` and `rememberSaveable`, and where would each fail you in your app?**
`remember` survives recomposition but **not** configuration change — rotate the device and it's gone. `rememberSaveable` survives configuration change via the saved-instance bundle, but only for types that are `Parcelable`/`Serializable` or have a custom `Saver`. In my Market screen, the search query lives in the ViewModel (survives rotation for free), so I don't need `rememberSaveable`. I'd reach for `rememberSaveable` for purely-UI things like a "details expanded" toggle that has no business meaning.

**Q10. How do you handle one-shot effects in Compose without firing them twice on recomposition?**
I collect from the `Channel`-backed flow inside a `LaunchedEffect(Unit)` (or keyed on the ViewModel) and handle each effect in the lambda. `LaunchedEffect` only relaunches if the key changes, so recomposition doesn't re-collect. Combined with `Channel.BUFFERED`, each effect is delivered exactly once.

**Q11. What causes unnecessary recompositions in a list like `CoinList`, and how do you avoid them?**
Three common causes: (1) passing **lambdas that get re-created** every recomposition — fix by `remember`-ing them or hoisting them stable; (2) passing **unstable types** (e.g., a `List` from a non-stable class) — Compose can't skip; mark data classes stable or use `ImmutableList`; (3) **missing keys** in `LazyColumn { items(coins, key = { it.id }) }` — without keys, Compose can't track item identity across reorders. I use `key = { it.id }` in my `CoinList`.

---

## Round 4 — Room, Retrofit, Error Handling

**Q12. How does your "single source of truth" pattern work in `CoinRepositoryImpl.refreshCoins()`?**
The UI never reads from the network directly. `getCoins()` returns `dao.getCoins()` — a `Flow` from Room. `refreshCoins()` hits the API, merges in the local `isFavorite` flag (so a refresh doesn't wipe the user's favorites), and writes everything back into Room. Room's flow re-emits and the UI updates automatically. Net effect: the DB is the only thing the UI trusts, and the network is just a way to keep the DB fresh.

**Q13. In `refreshCoins()` you specifically catch `HttpException` with `code() == 429` and surface a `RateLimitException`. Why not just lump it into NetworkException?**
Because the UI reacts differently. On a generic network error I show "retry". On a 429 I start a 60-second countdown (`startRateLimitCountdown`) and disable the refresh button — hammering CoinGecko makes it worse. Modeling it as a typed `AppException.RateLimitException` lets the ViewModel `when`-match cleanly without parsing strings.

**Q14. Why a sealed `AppResult` instead of letting exceptions bubble up from suspend functions?**
Exceptions are fine for *programmer errors*, but network failures, rate limits, and validation failures are **expected control flow**. Returning `AppResult.Success | Error | Loading` makes the ViewModel handle every case at compile time — `when` is exhaustive on a sealed class, so I can't forget to handle a 429. It also keeps the suspend function's contract honest: it doesn't throw under normal use.

**Q15. Walk me through your Room setup — entity, DAO, mapper. Why a separate `CoinEntity` and domain `Coin`?**
`CoinEntity` is annotated with `@Entity` and matches the DB schema. The domain `Coin` is a plain data class the rest of the app uses. They look similar today, but if I add a column tomorrow (say `lastSyncedAt`) the domain model shouldn't care — that's a persistence concern. The `Mapper.kt` `toDomain()` / `toEntity()` extension functions keep the boundary clean and let me change either side independently.

**Q16. What happens if I bump the Room schema without writing a migration?**
The app crashes on next launch with `IllegalStateException: Migration didn't properly handle...`. For a market-data app where the local cache is rebuildable from the network, I can use `.fallbackToDestructiveMigration()` — the user just re-fetches. For anything user-generated (favorites, portfolio) I'd write a real `Migration(from, to)` and bump `version` in `@Database`.

---

## Round 5 — Hilt / DI

**Q17. Walk me through your three Hilt modules.**
`NetworkModule` (`@InstallIn(SingletonComponent)`) provides `OkHttpClient` (with the logging interceptor), `Retrofit`, and the `Cryptoapi` interface. `DatabaseModule` provides the Room `AppDatabase` and `CoinDao`. `RepositoryModule` uses `@Binds` to map `CoinRepository` (interface) to `CoinRepositoryImpl`. ViewModels are annotated `@HiltViewModel` and Compose grabs them via `hiltViewModel()`.

**Q18. `@Binds` vs `@Provides` — why do you use `@Binds` for the repository?**
`@Binds` is for "this interface is implemented by that class" and Hilt generates the factory for free — zero runtime cost. `@Provides` is for "I'll write the construction logic myself" — needed for things like `Retrofit.Builder().build()`. Using `@Provides` for an interface-to-impl binding works but generates more code. Rule of thumb: if the constructor is `@Inject`-able, use `@Binds`.

**Q19. What's the difference between `SingletonComponent`, `ActivityRetainedComponent`, and `ViewModelComponent`?**
`SingletonComponent` lives the whole app process — Retrofit, Room, repositories. `ActivityRetainedComponent` survives configuration changes but dies with the Activity — useful for things that should outlive rotation but not the app. `ViewModelComponent` is scoped to a single ViewModel — for dependencies that should die when the ViewModel does.

---

## Round 6 — System Design Scenario

**Q20. Product wants real-time prices — sub-second updates instead of pull-to-refresh. How would you redesign the data layer?**
Replace polling with a **WebSocket**. Add a `PriceStreamSource` (OkHttp WebSocket or Ktor) that emits price ticks as a `Flow<PriceTick>`. The repository merges the stream into Room via a throttled write (`sample(500ms)`) so we don't thrash the DB. The UI flow stays the same — it still observes Room. Add a connection-state flow so the UI can show "Live / Reconnecting / Offline". Background: `WorkManager` is wrong for streaming; use a foreground service only if the user explicitly wants background updates, otherwise tear the socket down in `onStop`.

**Q21. The list scrolls poorly with 500 coins and sparkline images. How do you fix it?**
Profile first with the Layout Inspector + the Compose recomposition counts. Likely fixes: (1) `LazyColumn` with stable `key = { it.id }`; (2) data classes marked `@Immutable` or wrap lists in `ImmutableList` so Compose can skip; (3) hoist lambdas with `remember`; (4) Coil is already lazy — make sure I'm setting `size(...)` so it decodes at display resolution; (5) for sparklines, draw them on a `Canvas` instead of using `LazyRow` of dots; (6) paginate the API call (CoinGecko supports `per_page` + `page`) so initial load is faster.

**Q22. How would you test `MarketViewModel`?**
Unit test with `kotlinx-coroutines-test` and `MainDispatcherRule`. Mock the three use cases. For each intent I assert the resulting `state` and any `effect` emitted. For the debounce I use `runTest` with `advanceTimeBy(299)` (no filter yet) → `advanceTimeBy(2)` (filter fires). For Flow-based use cases, return `flowOf(fakeCoins)` from the mock. Repository goes against an in-memory Room DB + a `MockWebServer` for Retrofit. The Composables get screenshot tests via Paparazzi or Roborazzi for the obvious states (empty, loading, error, populated).

---

## Round 7 — Quickfire Kotlin / Android (rapid)

**Q23. `val` vs `const val`?** `const val` is compile-time, primitives/Strings only, top-level or in `companion object`; inlined at call sites. `val` is runtime-immutable; can be any type.

**Q24. `lateinit` vs `by lazy`?** `lateinit` is a `var` initialized later, throws if accessed first; no thread safety. `by lazy` is a `val` initialized on first access, thread-safe by default (`LazyThreadSafetyMode.SYNCHRONIZED`).

**Q25. Difference between `launch` and `async`?** `launch` returns `Job`, fire-and-forget. `async` returns `Deferred<T>`, awaitable, used when you need a result.

**Q26. `suspend` doesn't mean async — what does it mean?** It means the function can pause and resume without blocking the thread. The dispatcher decides which thread it runs on.

**Q27. `Dispatchers.Main` vs `Dispatchers.IO` vs `Dispatchers.Default`?** Main = UI thread. IO = blocking I/O (disk, network), large pool. Default = CPU-bound work (parsing, sorting), pool sized to cores.

**Q28. Why is `StateFlow` always hot and what does that mean for collectors?** It always has a current value and emits immediately to new collectors; collecting doesn't trigger work, the producer runs independently. Cold flows (`flow { }`) start running per collector.

**Q29. How does `collectAsStateWithLifecycle` differ from `collectAsState`?** The former pauses collection in `STOPPED` state — saves battery and prevents UI work while the screen is off. `collectAsState` keeps collecting regardless. Always prefer the lifecycle-aware version on screens.

**Q30. What is structured concurrency?** Child coroutines inherit their parent's scope — when the parent is cancelled, all children are cancelled, and the parent waits for children to complete. Prevents leaks; one of the strongest reasons to use coroutines over `Thread`/`AsyncTask`.

---

## Self-assessment rubric

If you can answer Q1–Q11 confidently with concrete code references = solid mid-level (4 YOE).
If you can also answer Q12–Q19 without hesitation = strong mid / approaching senior.
If you nail Q20–Q22 with trade-offs (not just a happy path) = senior signal.
Q23–Q30 should be near-instant; if any takes more than 5 seconds, drill it.

## Likely gaps to study before a real interview
1. **Pagination** (Paging 3 library) — your project doesn't use it; interviewers love it.
2. **WorkManager** — for any "background sync" question.
3. **Compose stability & skippability** — read the official "Stability in Compose" doc.
4. **Testing** — your project has only the default `ExampleUnitTest`; add real tests before claiming TDD.
5. **Multi-module Gradle** — you're single-module; be ready to explain how you'd split `:data`, `:domain`, `:feature-market`.
