# Features

> Version: 3.15.0

Statuses: **Done** · **Partial** · **Planned**

---

## Onboarding

| Feature | Status | Notes |
|---|---|---|
| Welcome screen | Done | |
| Profile setup (age, sex, height, weight, target weight) | Done | |
| Activity level + goal selection | Done | 5 levels, 5 goals |
| BMR / TDEE / macro calculation (Mifflin-St Jeor) | Done | Displayed on result screen |
| Profile and goals persistence on first launch | Done | |
| Database seeding (foods, exercises, programs) | Done | Runs once when food table is empty |

---


## Nutrition

### Daily journal

| Feature | Status | Notes |
|---|---|---|
| Date navigation (previous / next / today) | Done | Future dates disabled |
| Daily summary (calorie ring + macro bars) | Done | Over-target state: amber value + amber ring beyond goal (v3.11.0) |
| 4 meal sections (Breakfast / Lunch / Dinner / Snacks) | Done | |
| Add food to a meal | Done | |
| Delete food from a meal | Done | |
| Duplicate food consolidation (grouped display) | Done | |
| Save a meal as a favourite (meal template) | Done | Menu (⋮) of a filled meal; duplicate foods merged (v3.16.0) |
| Apply a favourite meal in one tap | Done | Menu (⋮) → picker; items appended, never replaced (v3.16.0) |
| Manage favourite meals | Done | Dedicated screen (kcal + foods + grams, delete); deletion never affects logged meals (v3.16.0) |
| Edit a favourite meal (rename / grams / add-remove) | Done | Tap a favourite → editor; add via food picker (search reused in "pick" mode); create from scratch via + (v3.16.0) |
| Copy daily summary to clipboard | Done | |
| Water intake tracking | Done | Quick-add 250 / 500 / 750 ml + custom |
| Configurable water goal | Done | In-screen dialog |
| Negative correction for water intake | Done | |

### Food search

| Feature | Status | Notes |
|---|---|---|
| Text search with debounce (300 ms) | Done | Food search only |
| Recent foods (shown when query is empty) | Done | |
| Favorite foods (shown when query is empty) | Done | |
| Food detail sheet | Done | Portion / grams mode toggle |
| "After adding" projected daily totals | Done | Color-coded: normal / warning / over target |
| Dietary filter applied to results | Done | |
| Category filter chips | Done | Single-select, loaded from DB, combinable with text |
| Custom food filter | Done | "Perso" toggle chip |
| Create custom food from search screen | Done | |
| Barcode scanner | Done | CameraX + ML Kit (bundled); local cache → OpenFoodFacts → manual, graceful offline fallback (v3.14.0) |
| Satiety / volume-eating signal | Done | Rule-based "Rassasiant/Peu rassasiant" label (density + protein + fibre) in food lists + portion sheet, plus a "Volume eating" sort in search; green→amber never red (v3.15.0) |

### Custom foods

| Feature | Status | Notes |
|---|---|---|
| Create food with macros per 100 g | Done | |
| Edit existing custom food | Done | |
| Delete custom food | Done | Hard delete if unused, soft-delete (archive) if referenced in history |
| Duplicate detection on create | Done | Warning dialog with option to create anyway |
| "Mes aliments" screen | Done | Searchable list of custom foods with edit and delete per item |
| "Perso" badge on food items | Done | Visible in all food lists |
| Dietary tags (pork, alcohol, vegetarian, vegan) | Done | Vegan implies vegetarian |
| French decimal separator support | Done | Accepts both `.` and `,` |

### Nutrition history

| Feature | Status | Notes |
|---|---|---|
| 60-day daily summaries | Done | kcal + protein / carbs / fat per day |
| Day detail (which foods were logged) | Done | Tap opens editable journal for that date |

---

## Workout

### Exercise catalog

| Feature | Status | Notes |
|---|---|---|
| ~150 seeded exercises | Done | |
| Exercise attachment variants | Partial | Naming convention "Exercice (attache)"; e.g. triceps extension corde/barre/overhead as distinct exercises, separate PR history. Seeder Phase 5 propagates renames (v3.19.0). A dedicated `variant` field + catalog grouping remains Planned. |
| Text search | Done | Accent-insensitive (nameNormalized), no debounce |
| Filter by muscle group (13 options) | Done | |
| Filter by type (5 options) | Done | |
| Exercise detail screen | Done | Muscles, equipment, description, progression chart |
| Exercise progression chart | Done | Canvas line chart, date X-axis, PR badge, 20 last sessions |
| Dual-mode behavior (standalone / builder) | Done | |
| Exercise favorites | Done | Heart icon toggle in standalone mode, Favoris filter chip |

### Session builder

| Feature | Status | Notes |
|---|---|---|
| Create workout template | Done | |
| Edit existing template | Done | |
| Add / remove exercises | Done | Duplicate prevention: already-added exercises are hidden in the catalog and disabled in the active-workout picker (v3.9.1) |
| Reorder exercises | Done | Up/down arrows per card, order persisted via orderIndex (v3.12.0) |
| Edit sets × reps per exercise | Done | |
| Link template to a program and day | Done | |

### Active workout tracker

| Feature | Status | Notes |
|---|---|---|
| Session timer (wall-clock, survives lock screen) | Done | |
| Tab navigation between exercises | Done | |
| Weight / reps input per set | Done | Select-all on focus to prevent appended digits |
| Duration input for cardio / isometric holds | Done | Per-exercise `trackingMode` (REPS_WEIGHT / DURATION / DURATION_WEIGHT) drives the input fields (v3.9.0) |
| Mark set as completed | Done | |
| Add / remove sets | Done | |
| Automatic rest timer (triggered on set completion) | Done | Duration configurable per exercise |
| Skip rest | Done | |
| Auto-save draft (debounced, 400 ms) | Done | |
| Resume interrupted session | Done | Dialog with elapsed time, staleness warning if > 24 h; idle gaps > 5 min excluded from recorded duration (v3.12.0) |
| In-progress workout banner on Sport screen | Done | Non-intrusive card above tabs when a draft exists (v3.9.1); stale state (> 24 h) with neutral tone + Abandonner action (v3.11.0) |
| In-session load reference (PR + last session) | Done | Per-exercise memory aid, weight modes only, hidden when no history (v3.10.0) |
| In-session last-session set detail | Done | Compact "Dernière fois : 10×6,8 · …" line under the reference (v3.13.0) |
| Finish session and save to history | Done | |

### Post-workout summary

| Feature | Status | Notes |
|---|---|---|
| Summary screen (duration, total volume, exercises) | Done | |
| Per-exercise set detail | Done | Tappable rows — edit reps / weight inline via dialog |

### Workout history

| Feature | Status | Notes |
|---|---|---|
| Past sessions list | Done | Tappable cards with formatted dates |
| Session detail view | Done | Name, date, duration, volume, sets per exercise, PR per exercise; set rows tappable to edit reps / weight |
| Weekly volume chart | Done | Bar chart, last 8 weeks, shown from 2 weeks of data |

### Training programs

| Feature | Status | Notes |
|---|---|---|
| Seeded program (Full Body 3 jours) | Done | PPL and Upper/Lower removed in v3.12.0 (empty shells); inactive programs deletable from their card |
| Activate / deactivate a program | Done | |
| Program detail (days and assigned templates) | Done | |
| Create / edit custom programs | Done | Full editor: name, description, duration, day assignments via template picker |
| Day-of / day-before reminders per program | Done | In ProgramDetailScreen |

---

## Calendar

| Feature | Status | Notes |
|---|---|---|
| Monthly grid with month navigation | Done | |
| Per-day indicators (nutrition logged, workout done, planned) | Done | Color-coded dots |
| Indicator legend | Done | |
| Day detail card (nutrition, water, sessions) | Done | |
| Link to nutrition screen from day detail | Done | |
| Workout frequency insight (4 weeks) | Done | Bar chart |
| 7-day average calorie insight | Done | Color-coded vs goal |

---

## Profile

| Feature | Status | Notes |
|---|---|---|
| Profile display (weight, height, goal, TDEE) | Done | |
| Nutrition goals display | Done | |
| Edit full profile | Done | Auto-recalculates macros on activity or goal change |
| Edit goals (manual or auto-calculated) | Done | Detailed breakdown before saving |
| Body weight log and trend chart | Done | Entry dialog, 30-day line chart, recent history list |
| Body weight card | Done | Last weight, date, delta vs previous; tappable → Suivi du poids; hidden if no entry |
| Reference weight clarification + one-tap resync | Done | Caption under TDEE; "Utiliser ma dernière pesée" when drift ≥ 1 kg; custom goals never overwritten (v3.11.0) |

### Weekly review (Bilan)

| Feature | Status | Notes |
|---|---|---|
| Bilan screen (entry from Profile) | Done | 7-day / 30-day period toggle; no schema change, aggregates existing data (v3.17.0) |
| Nutrition adherence | Done | Avg kcal/protein vs goal (over-target in amber), days in kcal target (90–105%), days protein reached (≥ goal), kcal/day bar chart |
| Weight trend | Done | Delta + kg/week rate, verdict vs profile goal direction (on-track / stalled / opposite / stable); empty state if < 2 weigh-ins |
| Training consistency | Done | Sessions this week vs active-program target, total over period, 4-week sessions/week bars |
| "À retenir" summary line | Done | Rule-based one-liner (no AI) |

---

## Settings

| Feature | Status | Notes |
|---|---|---|
| Link to Notifications screen | Done | |
| Theme selector (system / light / dark) | Done | Light theme added; persisted, applied live; system-bar contrast follows theme (v3.20.0) |
| Dietary filters (pork, alcohol, vegetarian, vegan) | Done | Persisted in DataStore |
| Halal mode info card | Done | |
| Data export (JSON) | Done | Via file picker |
| Data import / restore | Done | Confirmation dialog (destructive) |
| Version display | Done | Kalos 3.15.0 |

---

## Notifications

| Feature | Status | Notes |
|---|---|---|
| Smart reminders master switch | Done | Self-chaining OneTimeWorkRequest anchored to the chosen hour (v3.18.0) |
| Nutrition reminder (no meal logged today) | Done | |
| Activity reminder (configurable inactivity threshold: 2 / 3 / 5 days) | Done | |
| Hydration reminder (< 50% of water goal) | Done | |
| Smart reminder send time (slider, 6 h–22 h) | Done | Now reliably honored (was drifting via PeriodicWorkRequest) (v3.18.0) |
| Tap notification to open the app | Done | Deep-links to the relevant screen (nutrition / workout / water) (v3.18.0) |
| Per-program reminder toggle | Done | |
| Program reminder send time | Done | |
| Day-of / day-before reminder per program | Done | In ProgramDetailScreen |
