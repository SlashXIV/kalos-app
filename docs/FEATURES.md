# Features

> Version: 2.4.0

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

## Home

| Feature | Status | Notes |
|---|---|---|
| Daily calorie progress ring | Done | Consumed / Goal / Remaining |
| Daily macro bars (protein / carbs / fat) | Done | |
| Quick-action shortcuts (nutrition, workout) | Done | |
| Today's program workout card | Done | "Start session" button if workout scheduled today |
| Rest day card | Done | Shows next scheduled workout when no session today |
| Today's completed workouts list | Done | Name, exercise count, duration |

---

## Nutrition

### Daily journal

| Feature | Status | Notes |
|---|---|---|
| Date navigation (previous / next / today) | Done | Future dates disabled |
| Daily summary (calorie ring + macro bars) | Done | |
| 4 meal sections (Breakfast / Lunch / Dinner / Snacks) | Done | |
| Add food to a meal | Done | |
| Delete food from a meal | Done | |
| Duplicate food consolidation (grouped display) | Done | |
| Copy daily summary to clipboard | Done | |
| Water intake tracking | Done | Quick-add 250 / 500 / 750 ml + custom |
| Configurable water goal | Done | In-screen dialog |
| Negative correction for water intake | Done | |
| Smart food suggestions | Done | Based on remaining macros and dietary filters |

### Food search

| Feature | Status | Notes |
|---|---|---|
| Text search with debounce (300 ms) | Done | |
| Recent foods (shown when query is empty) | Done | |
| Favorite foods (shown when query is empty) | Done | |
| Food detail sheet | Done | Portion / grams mode toggle |
| "After adding" projected daily totals | Done | Color-coded: normal / warning / over target |
| Dietary filter applied to results | Done | |
| Create custom food from search screen | Done | |

### Custom foods

| Feature | Status | Notes |
|---|---|---|
| Create food with macros per 100 g | Done | |
| Edit existing custom food | Done | |
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
| ~100 seeded exercises | Done | |
| Text search | Done | |
| Filter by muscle group (13 options) | Done | |
| Filter by type (5 options) | Done | |
| Exercise detail screen | Done | Muscles, equipment, description, progression chart |
| Exercise progression chart | Done | Canvas line chart, date X-axis, PR badge, 20 last sessions |
| Dual-mode behavior (standalone / builder) | Done | |

### Session builder

| Feature | Status | Notes |
|---|---|---|
| Create workout template | Done | |
| Edit existing template | Done | |
| Add / remove exercises | Done | |
| Edit sets × reps per exercise | Done | |
| Link template to a program and day | Done | |

### Active workout tracker

| Feature | Status | Notes |
|---|---|---|
| Session timer (wall-clock, survives lock screen) | Done | |
| Tab navigation between exercises | Done | |
| Weight / reps input per set | Done | |
| Mark set as completed | Done | |
| Add / remove sets | Done | |
| Automatic rest timer (triggered on set completion) | Done | Duration configurable per exercise |
| Skip rest | Done | |
| Auto-save draft (debounced, 400 ms) | Done | |
| Resume interrupted session | Done | Dialog with elapsed time, staleness warning if > 24 h |
| Finish session and save to history | Done | |

### Post-workout summary

| Feature | Status | Notes |
|---|---|---|
| Summary screen (duration, total volume, exercises) | Done | |
| Per-exercise set detail | Done | |

### Workout history

| Feature | Status | Notes |
|---|---|---|
| Past sessions list | Done | Tappable cards with formatted dates |
| Session detail view | Done | Name, date, duration, volume, sets per exercise, PR per exercise |
| Volume / progression charts | Planned | Weekly volume aggregation, deferred |

### Training programs

| Feature | Status | Notes |
|---|---|---|
| Display 3 seeded programs (PPL, Upper/Lower, Full Body) | Done | |
| Activate / deactivate a program | Done | |
| Program detail (days and assigned templates) | Done | |
| Create / edit custom programs | Partial | UI incomplete |
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

---

## Settings

| Feature | Status | Notes |
|---|---|---|
| Link to Notifications screen | Done | |
| Dietary filters (pork, alcohol, vegetarian, vegan) | Done | Persisted in DataStore |
| Halal mode info card | Done | |
| Data export (JSON) | Done | Via file picker |
| Data import / restore | Done | Confirmation dialog (destructive) |
| Version display | Done | Kalos 2.1.2 |

---

## Notifications

| Feature | Status | Notes |
|---|---|---|
| Smart reminders master switch | Done | WorkManager daily job |
| Nutrition reminder (no meal logged today) | Done | |
| Activity reminder (configurable inactivity threshold: 2 / 3 / 5 days) | Done | |
| Hydration reminder (< 50% of water goal) | Done | |
| Smart reminder send time (slider, 6 h–22 h) | Done | |
| Per-program reminder toggle | Done | |
| Program reminder send time | Done | |
| Day-of / day-before reminder per program | Done | In ProgramDetailScreen |
