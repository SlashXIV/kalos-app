# Changelog

---

## v2.2.0 — 23 May 2026

### Added
- Workout history list: cards are now tappable with formatted dates (Aujourd'hui / Hier / d MMM [yyyy])
- Session detail screen (`WorkoutLogDetailScreen`): oriented toward reading past performance
  - Header: session name, full date, duration, total volume, set count
  - Per-exercise cards: name, primary muscle group, completed sets (reps × weight), volume sub-total
  - Personal record: "PR : X.X kg" sourced from `getMaxWeight()` per exercise, displayed inline
- Route `workout/log/{logId}` wired in the navigation graph; distinct from the post-workout summary route

---

## v2.1.2 — 23 May 2026

### Added
- Body weight log (`Profile > Suivi du poids`): entry dialog, 30-day Canvas line chart, recent history list with per-entry delta. Last logged weight surfaced as a subtitle on the Profile navigation card.

### Fixed
- Hydration block in the daily journal now follows the selected date instead of always showing today's data. Viewing a past date displays the water intake logged that day. Quick-add buttons and the edit-goal icon are hidden when browsing history; the progress bar and total remain visible in read-only mode.
- `buildDailySummary` (copy to clipboard) now uses the date-scoped water value, consistent with the rest of the journal.
- Body weight log now enforces one entry per day: logging a second weight on the same day replaces the existing value instead of creating a duplicate row. Button label and dialog title switch to "Mettre à jour" when a today entry already exists.

### Changed
- `WaterRepository` is now injected into `NutritionViewModel` as the single source of truth for the selected date. `WaterViewModel` is no longer instantiated from `NutritionScreen`.

---

## v2.1.1 — 23 May 2026

### Added
- Notifications screen (`Settings > Notifications`) centralizing all reminder configuration
- Smart reminders moved from Settings to Notifications screen
- Per-program reminder toggles with a direct link to program detail
- Program reminder send time configurable from the Notifications screen
- "After adding" projected macro strip in the food detail sheet — shows projected daily totals vs goal before confirming, with color coding (normal / warning at > 90% / over target)

### Changed
- `secondary` color token remapped from orange (`#FB923C`) to green in `Color.kt` — eliminates all residual brown across the UI
- Slider inactive ticks: explicit `inactiveTrackColor` and `inactiveTickColor` set on both sliders in NotificationsScreen

### Fixed
- Filter chips in ExerciseCatalogScreen and ActiveWorkoutScreen now use `primaryContainer` for selected state consistently
- CalendarGrid cells memoized with `remember(month)` for performance
- Removed deprecated `distinctUntilChanged()` calls on StateFlow in NutritionViewModel

---

## v2.1.0 — 22 May 2026

### Added
- Smart reminders system (`IntelligentReminderScheduler` + `IntelligentReminderWorker` via WorkManager)
  - Nutrition reminder: triggers if no meal logged today
  - Activity reminder: triggers if inactive for a configurable number of days (2 / 3 / 5)
  - Hydration reminder: triggers if water intake < 50% of daily goal
  - Send time configurable via slider (6 h–22 h, default 20 h)
  - Master switch + per-type toggles
  - Dedicated notification channel `kalos_smart_reminders`

### Fixed
- Workout frequency bar chart: "Cette sem." label was vertically clipped — fixed by removing fixed container height and reserving a dedicated label area

---

## v2.0.0 — 22 May 2026

### Added
- Active workout draft auto-saved every 400 ms (debounced) to survive process death
- Resume dialog when an interrupted session is found, with elapsed time display and staleness warning if over 24 hours old
- Post-workout summary screen: duration, total volume, and per-exercise set detail
- Total volume (sum of reps × weight) calculated and persisted on WorkoutLog

---

## v1.9.2 — 22 May 2026

### Fixed
- Duplicate foods in a meal are now consolidated and displayed as a single grouped item
- Data import: fixed restore logic

---

## v1.9.0–v1.9.1

### Added
- Data export to JSON via file picker
- Data import / restore with confirmation dialog (destructive operation)
- Copy daily nutrition summary to clipboard

---

## v1.8.0

### Added
- Dietary tags on custom foods (pork, alcohol, vegetarian, vegan) with interdependency logic
- Dietary filters in Settings, applied to smart suggestions and food search results

---

## v1.7.0

### Added
- Training programs: 3 seeded programs (PPL 6-day, Upper/Lower 4-day, Full Body 3-day)
- Program activation and display on Home and Calendar
- Day-of and day-before reminder configuration per program

---

## v1.0.0–v1.6.1

### Added
- Onboarding flow with TDEE and macro calculation (Mifflin-St Jeor)
- Daily nutrition journal with 4 meal sections
- Food search with recent, favorites, and seeded database (~300 foods)
- Custom food creation with macros per 100 g
- Water intake tracking with quick-add buttons
- Smart food suggestions based on remaining macros
- Exercise catalog (~100 exercises) with muscle group and type filters
- Workout template builder
- Active workout tracker with rest timer
- Monthly calendar with nutrition and workout indicators
- Workout frequency and average calorie insights
- Profile setup with goal and activity level
- Editable profile and nutrition goals with auto-recalculation
- Material3 dark theme, green / orange palette, Nunito typeface
- French localization throughout
