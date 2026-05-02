# MarketTracker — Learning Roadmap

A class-by-class walkthrough of the entire Android codebase. Read it top-to-bottom and you will understand every file, why it exists, and how it connects to the rest of the app.

---

## 1. The 30-second overview

**What the app does:** Shows live cryptocurrency prices, lets the user search/filter them, favourite coins, and view a detail screen with a price chart.

**Tech stack**

| Concern | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVI + Clean Architecture (3 layers) |    
| Dependency Injection | Hilt (Dagger) |
| Networking | Retrofit + OkHttp + Gson |
| Local database | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |  
| Navigation | Jetpack Navigation Compose |
| Image loading | Coil |
| Splash animation | Lottie |

**Data source:** The free CoinGecko REST API (`https://api.coingecko.com/api/v3/`). No auth key, but rate-limited to about 10–30 calls/minute — which is why the code handles the HTTP 429 rate-limit case explicitly.

---

## 2. The three layers (Clean Architecture)

Everything in `app/src/main/java/com/example/markettracker/` is organised into these layers. Think of them as **rings** — outer rings depend inward, never the other way around.

```
 ┌─────────────────────────────────────────────────────┐
 │  PRESENTATION (ui/)                                 │
 │    Composables + ViewModels + State/Intent/Effect   │
 │    ──> depends on domain                            │
 ├─────────────────────────────────────────────────────┤
 │  DOMAIN (domain/)                                   │
 │    Coin model + UseCases + Repository INTERFACE     │
 │    ──> depends on nothing Android-specific          │
 ├─────────────────────────────────────────────────────┤
 │  DATA (data/)                                       │
 │    Retrofit API + Room DB + Repository IMPL         │
 │    ──> implements the domain repository contract    │
 └─────────────────────────────────────────────────────┘
```

**Why this matters for reading the code:** If you start from the UI you'll hit ViewModels, which call UseCases, which call an `ICoinRepository` interface. Only at the deepest level do you reach Retrofit and Room. That's the correct reading direction — from outside in.

---

## 3. The recommended reading order

Follow this order. Each step builds on the previous one.

```
① Entry points       →  MainActivity.kt, MarketTrackerApp.kt, AndroidManifest.xml
② Core contracts     →  core/AppResult.kt, core/AppException.kt
③ Data layer         →  data/api, data/remote/dto, data/db, data/repository
④ Domain layer       →  domain/model, domain/usecase
⑤ DI wiring          →  di/NetworkModule, di/DatabaseModule, di/RepositoryModule
⑥ Navigation         →  navigation/NavGraph.kt
⑦ Theme & components →  ui/theme, ui/components
⑧ Screens (in order) →  splash → login → dashboard → market → coin detail
⑨ Unfinished screens →  portfolio, invest  (still hardcoded — good exercise)
```

---

## 4. Class-by-class explanation

### ① Entry points

#### `MarketTrackerApp.kt`
```kotlin
@HiltAndroidApp
class MarketTrackerApp : Application()
```

**What:** The custom `Application` class. Registered in `AndroidManifest.xml` via `android:name=".MarketTrackerApp"`.

**How:** `@HiltAndroidApp` tells Hilt to generate the top-level dependency-injection component (`SingletonComponent`) at compile time. All `@Singleton` objects (Retrofit, Room, repositories) are created once here and live as long as the app process lives.

**Why:** Without this, every `@HiltViewModel`, `@AndroidEntryPoint`, and `@Inject` in the project would fail at runtime. It's the entry door for the whole DI graph.

#### `MainActivity.kt`
```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() { ... setContent { CryptoAppTheme { NavGraph() } } }
```

**What:** The single `Activity` in the whole app. Every screen is a `@Composable` inside `NavGraph`.

**How:** `enableEdgeToEdge()` lets the app draw behind the system bars. `setContent { ... }` is Compose's entry point — from this moment on, the UI is pure Compose, no XML.

**Why one Activity?** Modern Android apps use a single-Activity architecture so navigation state, back-stack, and ViewModels are all managed by the Compose Navigation library, not the Android framework's activity stack. It's simpler, faster, and easier to test.

---

### ② Core contracts (`core/`)

#### `core/AppResult.kt`

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val exception: AppException) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}
```

**What:** A generic wrapper for every data operation's outcome.

**How:** Being a `sealed class`, the Kotlin compiler enforces exhaustive `when` branches — the UI cannot forget to handle `Error` or `Loading`.

**Why:** Replaces the old habit of `try { ... } catch (Exception) { e.printStackTrace() }`. Every screen knows exactly how to react, no silent failures.

#### `core/AppException.kt`

```kotlin
sealed class AppException(message, cause) : Exception(message, cause) {
    class NetworkException(cause)         // no internet / timeout
    class ServerException(code, body)     // HTTP 4xx/5xx
    class RateLimitException              // HTTP 429 (CoinGecko free tier)
    class ParseException(cause)           // JSON malformed
    class UnknownException(cause)         // anything else
}
```

**What:** A typed hierarchy of domain-level errors.

**How:** `CoinRepositoryImpl.refreshCoins()` catches raw `IOException` / `HttpException` / generic `Exception` and translates each into the right subtype.

**Why:** The UI can now show *specific* messages ("no internet" vs "too many requests — wait a minute") without caring about the underlying HTTP plumbing.

---

### ③ Data layer (`data/`)

The data layer knows about networking and databases. Nothing else in the project does.

#### `data/api/Cryptoapi.kt`

```kotlin
interface Cryptoapi {
    @GET("coins/markets")
    suspend fun getCoins(
        @Query("vs_currency") currency: String = "inr",
        @Query("sparkline")   sparkline: Boolean = true
    ): List<CoinDto>

    @GET("coins/{id}")
    suspend fun getCoinInfo(@Path("id") id: String, ...): CoinInfoDto
}
```

**What:** The Retrofit service definition for CoinGecko.

**How:** Retrofit reads the annotations (`@GET`, `@Query`, `@Path`) at runtime and generates the HTTP call for you — you never write `URL`, `HttpURLConnection`, or raw `OkHttp` code. The `suspend` keyword makes each call a coroutine, so it yields the thread while the network does its work.

**Why an interface?** So tests can replace it with a fake that returns canned JSON — no live network needed.

#### `data/remote/dto/` — the three DTOs

- **`CoinDto`** — the JSON shape of one coin from `coins/markets`. `@SerializedName` maps snake_case JSON keys to camelCase Kotlin properties (e.g. `current_price` → `currentPrice`).
- **`SparklineDto`** — wraps `price: List<Double>`, the 7-day price series shown as the mini chart.
- **`CoinInfoDto`** + `DescriptionDto` — the shape of the `coins/{id}` response; only the English description is read.

**Why DTOs exist separately from the `Coin` domain model:** if CoinGecko renames a field, or the project later switches to Binance, you only touch the DTO and the mapper. Nothing in the ViewModels or UI changes.

#### `data/db/CoinEntity.kt`

```kotlin
@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,
    val name, symbol, image, ...
    val isFavorite: Boolean = false,
    val sparklineJson: String = "[]"      // price list stored as JSON text
)
```

**What:** The Room table definition.

**How:** Room reads `@Entity` and `@PrimaryKey` at compile time (via KSP) and generates SQL `CREATE TABLE coins (...)` for you.

**Why `sparklineJson` is a String:** Room doesn't support `List<Double>` natively without a TypeConverter. Storing the list as a JSON string (using Gson, which is already on the classpath) is a pragmatic shortcut.

#### `data/db/CoinDao.kt`

```kotlin
@Dao
interface CoinDao {
    @Insert(onConflict = REPLACE)           suspend fun insertCoins(coins: List<CoinEntity>)
    @Query("SELECT * FROM coins")           fun getCoins(): Flow<List<CoinEntity>>
    @Query("SELECT ... WHERE id = :coinId") fun getCoinById(coinId): Flow<CoinEntity?>
    @Query("UPDATE coins SET isFavorite = :isFav WHERE id = :coinId")
                                            suspend fun updateFavorite(coinId, isFav)
}
```

**What:** All SQL the app will ever run.

**How:** Anything returning `Flow` is reactive — Room emits a new value whenever the underlying row/table changes. That is the secret behind the UI auto-updating after a network refresh.

**Why REPLACE on conflict:** fetching 100 fresh coins will simply overwrite the existing 100 rows by primary key — no manual upsert logic. The minor caveat: `isFavorite` is lost on replace; the TODO in `Mapper.kt` flags this.

#### `data/db/AppDatabase.kt`

```kotlin
@Database(entities = [CoinEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coinDto(): CoinDao
}
```

**What:** Lists every table and the DB version. `version = 3` means the schema has been bumped twice already.

**Why `fallbackToDestructiveMigration()` in the module:** easier during development but wipes all saved data (including favourites) on a schema change. Before shipping, replace with proper `Migration` objects.

#### `data/db/DatabaseProvider.kt`
An older manual singleton (double-checked locking). **Currently unused** — Hilt's `DatabaseModule` provides the DB instead. It's safe to delete after confirming nothing references it. Good learning artifact because it shows what Hilt replaced.

#### `data/db/Mapper.kt`

Two extension functions:

```kotlin
fun CoinDto.toEntity(): CoinEntity        // API  → Room
fun CoinEntity.toDomain(): Coin           // Room → Domain
```

**Why:** Converts between the three incompatible shapes. Every screen that shows a coin goes through `toDomain()`. If you ever rename a DB column, this is the only file you need to change.

#### `data/repository/ICoinRepository.kt`

```kotlin
interface ICoinRepository {
    val coins: Flow<List<Coin>>
    suspend fun refreshCoins(): AppResult<Unit>
    suspend fun toggleFavorite(coinId, currentState): AppResult<Unit>
    fun getCoinById(coinId): Flow<Coin?>
}
```

**What:** The **contract** every other layer talks to. ViewModels and UseCases depend on this interface — **not** the concrete class.

**Why an interface?** Testability (inject a fake in unit tests) and swappability (swap CoinGecko for Binance without touching ViewModels).

#### `data/repository/CoinRepositoryImpl.kt`

The single source of truth for coin data.

```kotlin
@Singleton
class CoinRepositoryImpl @Inject constructor(
    private val api: Cryptoapi,
    private val dao: CoinDao
) : ICoinRepository {

    override val coins: Flow<List<Coin>> =
        dao.getCoins().map { it.map { e -> e.toDomain() } }

    override suspend fun refreshCoins(): AppResult<Unit> = try {
        dao.insertCoins(api.getCoins().map { it.toEntity() })
        AppResult.Success(Unit)
    } catch (e: IOException)      { AppResult.Error(AppException.NetworkException(e)) }
      catch (e: HttpException)    { if (e.code() == 429) RateLimit else Server }
      catch (e: Exception)        { AppResult.Error(AppException.UnknownException(e)) }

    override suspend fun toggleFavorite(...) = ...
    override fun getCoinById(coinId) = dao.getCoinById(coinId).map { it?.toDomain() }
}
```

**How:** `refreshCoins()` is a "network → DB → return result" pipeline. Crucially, the UI **never reads the network response directly**. It only observes the DB. This is called the *single source of truth* pattern.

**Why `@Singleton`:** two repositories with separate in-memory state would desynchronise. One, ever.

---

### ④ Domain layer (`domain/`)

Pure Kotlin, no Android dependencies. This is where the business rules live.

#### `domain/model/Coin.kt`

```kotlin
data class Coin(
    id, name, symbol, imageUrl,
    currentPrice: Double,
    priceChangePercent24h: Double,
    isFavorite: Boolean = false,
    sparkline: List<Float> = emptyList()
)
```

**What:** The single shape every screen/ViewModel uses.

**Why:** With `Coin` decoupled from `CoinDto` and `CoinEntity`, swapping data sources or adding columns is a mapper change, not a project-wide refactor.

#### `domain/model/MarketFilter.kt`

```kotlin
enum class MarketFilter { ALL, FAVORITES }
```

Lives in `domain/` so `SearchCoinsUseCase` can use it without dragging in a UI dependency.

#### `domain/usecase/` — seven single-purpose classes

Each use case does exactly one thing and can be injected anywhere.

| Use case | Returns | What it does |
|---|---|---|
| `GetCoinsUseCase` | `Flow<List<Coin>>` | Live stream of every coin in Room. |
| `GetCoinDetailUseCase` | `Flow<Coin?>` | Live stream of one coin by id. `null` until the DB has that coin. |
| `GetTopGainersUseCase` | `Flow<List<Coin>>` | Filters to `priceChange > 0`, sorts desc, takes top N. |
| `GetTopLosersUseCase` | `Flow<List<Coin>>` | Mirror image — negative change, sorted ascending. |
| `RefreshCoinsUseCase` | `AppResult<Unit>` | Triggers a network fetch + DB save. |
| `ToggleFavoriteUseCase` | `AppResult<Unit>` | Flips `isFavorite` for a given id in the DB. |
| `SearchCoinsUseCase` | `Flow<List<Coin>>` | Applies `MarketFilter` + text search over the coin list. (Currently duplicated in `MarketViewModel.observeFilteredCoins()` — good consolidation target.) |

**How:** The `operator fun invoke(...)` trick makes each one callable like a function: `getCoins()` instead of `getCoins.invoke()`.

**Why so many small classes?** Testability and single-responsibility. You can unit-test "top gainers returns top 5 sorted desc" without any ViewModel, database, or Android runtime.

---

### ⑤ DI wiring (`di/`)

Hilt takes the contracts (`ICoinRepository`, `CoinDao`, `Cryptoapi`) and decides which concrete object to hand out.

#### `di/NetworkModule.kt`

Provides, as singletons:

1. `OkHttpClient` — 30-second timeouts, HTTP logging interceptor (prints every request/response to Logcat while debugging).
2. `Retrofit` — base URL = `https://api.coingecko.com/api/v3/`, uses Gson for JSON parsing.
3. `Cryptoapi` — Retrofit generates the implementation via `retrofit.create(Cryptoapi::class.java)`.

#### `di/DatabaseModule.kt`

Provides `AppDatabase` (built with `Room.databaseBuilder`) and `CoinDao`. Uses `@ApplicationContext` so we never leak an Activity context into a singleton.

#### `di/RepositoryModule.kt`

```kotlin
@Binds @Singleton
abstract fun bindCoinRepository(impl: CoinRepositoryImpl): ICoinRepository
```

**Why `@Binds` instead of `@Provides`:** it's a compile-time alias — cheaper than generating a factory method. It tells Hilt: "whenever anyone asks for `ICoinRepository`, hand them the `CoinRepositoryImpl` you already know how to construct (via its `@Inject` constructor)."

---

### ⑥ Navigation (`navigation/NavGraph.kt`)

Two-level navigation.

**Level 1: root graph** (in `NavGraph()`)

```
splash ──▶ login ──▶ main ──▶ coin_detail/{coinId}
```

Each transition uses `popUpTo(...) { inclusive = true }` so the user can't press Back and return to the splash or login.

**Level 2: bottom-nav graph** (in `MainApp()`)

```
home  |  market  |  invest  |  portfolio
```

Defined as a sealed class `BottomNavItem` with four objects. Tapping a tab uses the standard pattern `popUpTo(startDestination) { saveState = true }` + `launchSingleTop = true` + `restoreState = true` so each tab preserves its scroll position.

**`coin_detail/{coinId}`** takes a string argument declared via `navArgument("coinId") { type = NavType.StringType }`. Jetpack Navigation puts this into the `SavedStateHandle` of `CoinDetailViewModel` automatically — zero glue code.

---

### ⑦ Theme & reusable components

#### `ui/theme/Color.kt`
A palette of named colours: `PrimaryGreen`, `PrimaryRed`, `NeonGreen`, `TextPrimary`, `CardBackground`, etc. All semantic, so swapping to a light theme later is one file.

#### `ui/theme/Theme.kt`
```kotlin
@Composable fun CryptoAppTheme(content) { MaterialTheme(colorScheme = DarkColorScheme, content) }
```
Pins the app to a dark colour scheme. Wraps everything inside `MainActivity.setContent { ... }`.

#### `ui/theme/Type.kt`
Default Material 3 `Typography`. Placeholder — a real design system would customise font families and sizes here.

#### `ui/components/AppBackground.kt — StarfieldBackground`
A standalone animated `Canvas` that draws 120 twinkling stars using a single infinite `rememberInfiniteTransition`. Each star has a random phase so brightnesses ripple independently. Used only on the `CoinDetailScreen` to give it the "deep-space" look.

---

### ⑧ Screens

#### `ui/screens/splash/SplashScreen.kt`
Plays a Lottie animation once, navigates to login exactly when progress hits `1f`.

**Why no hardcoded delay:** `delay(2500)` is a guess; `animateLottieCompositionAsState` is frame-accurate and automatically syncs with the animation file.

---

#### `ui/screens/login/*` — 4 files

**`LoginState.kt`** — `(userName, password, isLoading, error)`. Note the author removed `isLoginSuccessful` on purpose — leaving it in `State` caused a double-navigation bug on screen rotation.

**`LoginEffect.kt`** — `sealed class LoginEffect { data object NavigateToDashboard }`. One-shot events travel through a `Channel` so rotation can't replay them.

**`LoginViewModel.kt`**
- `state: StateFlow<LoginState>` — read by the UI.
- `effect: Flow<LoginEffect>` — one-shot events.
- `onUserNameChange()` / `onPasswordChange()` / `login()` handlers.
- `login()` only validates "non-empty fields" right now. The TODO block at the top shows exactly how to plug in a real `AuthUseCase` later.

**`LoginScreen.kt`** — a `Column` with two `OutlinedTextField`s and a `Button`. Crucially collects `viewModel.effect` inside `LaunchedEffect(Unit)` so navigation fires **exactly once** per successful login.

---

#### `ui/screens/dashboard/*` — 8 files

The Home tab. Architecturally, the second-most important flow after Market.

- **`DashboardScreen.kt`** — composes `TopBar`, `PortfolioCard`, `QuickActionsRow`, `TopGainersSection(state.topGainers)`, `TopLosersSection(state.topLosers)` inside a `LazyColumn`.
- **`DashboardViewModel.kt`** — spins up two `collect` coroutines for `GetTopGainersUseCase(5)` and `GetTopLosersUseCase(5)`. Also calls `refreshCoins()` in `init {}` so the dashboard can render even if the user opens it before visiting the Market tab.
- **`DashboardState.kt`** — `(topGainers, topLosers, isLoading)`.
- **`TopBar.kt`** — just a `Text("Home", displayMedium)`.
- **`PortfolioCard.kt`** — **still mocked**: hardcoded `"₹100,000.00"` and `"Portfolio growth 28%"`. Good next exercise.
- **`QuickActionsRow.kt`** — SIP / Earn / Exchange / Market icons, static UI only.
- **`TopGainersSection.kt`** — shows a loading placeholder when empty, otherwise iterates `coins.forEach { DashboardCoinItem(it) }`.
- **`TopLosersSection.kt`** — same but reuses `DashboardCoinItem` (colour is computed from the sign of `priceChangePercent24h`).

---

#### `ui/screens/market/*` — 8 files (the MVI showcase)

This is the most instructive area of the project. Everything about MVI and Clean Architecture comes together here.

**`MarketIntent.kt`** — every action the user can take:
```
SearchChanged(query) | FilterChanged(filter) | FavoriteClicked(id, current) | Refresh | CoinClicked(id)
```

**`MarketState.kt`** — a single snapshot: `coins, searchQuery, filter, isLoading, error, rateLimitCountdown`. Updates happen via immutable `copy(...)`.

**`MarketEffect.kt`** — `NavigateToCoinDetail(coinId)` and `ShowError(message)`. Sent through a `Channel`, not stored in `State`.

**`MarketViewModel.kt`** (`ui/viewmodel/MarketViewModel.kt`)

This one is worth reading twice. The important patterns:

1. **Three internal flows combined into one**: `combine(getCoins(), _searchQuery, _filter) { ... }` re-emits whenever any of the three change.
2. **Debounced search**: `onIntent(SearchChanged)` updates the UI immediately for responsiveness, but delays the *filter input* by 300 ms so typing "bitcoin" fires one filter pass, not six.
3. **Rate-limit countdown**: when the API returns 429, `startRateLimitCountdown(60)` starts a coroutine that ticks state down each second. The UI reads `state.rateLimitCountdown` and shows `RateLimitBanner` when > 0.
4. **Everything flows through `onIntent()`**: the single public entry point — the UI never calls private methods.

**`MarketScreen.kt`** — the Composable. Structure:

```
Column
├── Text("Markets")
├── MarketSearchBar(query, onQueryChange)
├── RateLimitBanner (conditional)
├── MarketFilterRow(coins, currentFilter, onFilterSelected)
└── MarketCoinsList(...)          ── LazyColumn of MarketCoinItem
        └── MarketCoinItem
              ├── AsyncImage (Coil)     ── coin logo
              ├── Column: name + symbol
              ├── SparklineChart        ── 20-point canvas mini-chart
              ├── Column: price + change
              └── IconButton (favourite)
```

All sub-composables are **stateless** — they receive data + lambdas. Only `MarketScreen` holds a reference to the ViewModel. That makes every sub-composable independently previewable/testable.

`SparklineChart` is a 50-line Compose `Canvas` that draws a smooth line + translucent fill under it. Colour = green/red depending on `isPositive`.

**`CoinList.kt`** — legacy DTO used only by the hardcoded `InvestScreen`. Can be removed once `InvestScreen` is wired to real data.

---

#### `ui/screens/market/CoinDetailState|ViewModel|Screen.kt`

Reached via `coin_detail/{coinId}` from the market list.

**`CoinDetailState.kt`** — `(coin: Coin?, isLoading, error, selectedTimeframe, description, isDescriptionLoading)`.

**`CoinDetailViewModel.kt`** — three jobs:
1. Read `coinId` from `SavedStateHandle` (populated automatically from the nav arg).
2. `loadCoin()` — collect `GetCoinDetailUseCase(coinId)` from Room and push into state.
3. `fetchDescription()` — one-off call to `api.getCoinInfo(coinId)`, strips HTML tags with a regex, stores result.
4. `onTimeSelected` / `getChartData()` — slices the 7-day sparkline (`takeLast(20/40/60/120)`) depending on the chosen timeframe; `smoothData` applies a 3-point rolling average.

**`CoinDetailScreen.kt`** — the screen where the most visual fireworks happen:
- `StarfieldBackground` behind everything
- Coin header (logo + name + symbol)
- Price + change
- A beautiful animated smooth-curve chart drawn with `path.cubicTo(...)`, plus glow and gradient fill
- Timeframe selector (15m / 30m / 1H / 4H / 1D) — taps call `viewModel.onTimeSelected`
- Scrollable "About" description (with `LinearProgressIndicator` while loading)
- BUY / SELL outlined buttons (wired to `TODO`)

---

#### `ui/screens/portfolio/PortfolioScreen.kt`
**Still hardcoded**: 5 fake `HoldingItem`s, a mocked summary card, BUY/WITHDRAW buttons that do nothing. Perfect next exercise: create `PortfolioViewModel` + `PortfolioState`, store holdings in Room, expose them via a `GetHoldingsUseCase`. The existing structure gives you the blueprint.

#### `ui/screens/invest/InvestScreen.kt`
Similar status — uses the old `CoinList` DTO with literal strings. Replace with a `TrendingCoinsUseCase` fed by the same repository.

---

## 5. The data flow, traced end-to-end

Say the user opens the app and taps Bitcoin in the list.

```
1.  MarketTrackerApp created by Android                        (Hilt component initialised)
2.  MainActivity.onCreate  →  setContent { NavGraph() }
3.  NavGraph route "splash" renders SplashScreen
4.  Lottie finishes → navigate("login") → LoginScreen
5.  User taps Login → LoginViewModel.login() sends LoginEffect.NavigateToDashboard
6.  NavGraph navigates to "main" → MainApp() shows bottom nav, starting on Home
7.  DashboardScreen composes → hiltViewModel() builds DashboardViewModel
      └─ init { } calls RefreshCoinsUseCase()
           └─ CoinRepositoryImpl.refreshCoins()
                 ├─ api.getCoins()               ← Retrofit + OkHttp
                 ├─ map DTOs → Entities          ← Mapper.kt
                 └─ dao.insertCoins(entities)    ← Room
8.  Room emits the new list  →  coins Flow  →  GetTopGainersUseCase Flow
      → DashboardState.topGainers  → recompose → real coin data appears
9.  User taps the Market tab (same repository already populated)
10. User taps "Bitcoin" row
      → MarketIntent.CoinClicked("bitcoin")
      → MarketEffect.NavigateToCoinDetail("bitcoin")
      → navController.navigate("coin_detail/bitcoin")
11. CoinDetailViewModel reads "bitcoin" from SavedStateHandle
      → collects GetCoinDetailUseCase("bitcoin")  (same Room flow)
      → fires api.getCoinInfo("bitcoin") for the description
12. UI renders: logo, price, chart, description, BUY/SELL buttons
```

Every arrow above is a class you have already read.

---

## 6. Where to go next (suggested edits)

In order of increasing difficulty:

1. **Delete** `data/db/DatabaseProvider.kt` — superseded by `DatabaseModule`.
2. **Wire `MarketViewModel` to use `SearchCoinsUseCase`** instead of duplicating the filter logic. A one-file refactor.
3. **Fix the `isFavorite` wipe on refresh** noted in `Mapper.kt` — use a smart upsert or merge favourites before inserting.
4. **Replace `fallbackToDestructiveMigration()` in `DatabaseModule`** with proper `Migration(2→3)` before the first release.
5. **Convert `PortfolioScreen` to real data** using the MVI pattern already proven on Market.
6. **Plug a real auth backend into `LoginViewModel`** following the TODO block it ships with.
7. **Add a Snackbar host** so `MarketEffect.ShowError` actually shows to users (currently a TODO in `MarketScreen`).
8. **Add unit tests** for the seven use cases — they're pure functions, trivial to test with a fake `ICoinRepository`.

---

## 7. Glossary of one-liners

- **MVI (Model-View-Intent):** State flows down, Intents flow up, one-shot Effects are fired and forgotten.
- **Clean Architecture:** UI → domain → data. Outer rings depend on inner rings.
- **Hilt:** Dagger DI with Android-aware annotations (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`).
- **Flow:** Kotlin's reactive stream. Room returns `Flow` so the UI auto-updates.
- **Single source of truth:** the UI reads from Room. The network writes to Room. Those are the only two data channels.
- **Channel vs StateFlow:** StateFlow replays the latest value on every collector — great for UI state, terrible for navigation. Channel delivers each value exactly once — perfect for navigation effects.

---

*End of roadmap. Read it twice, then open `MainActivity.kt` and follow the arrows.*
