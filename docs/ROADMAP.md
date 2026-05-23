# Roadmap

> Last updated: v2.1.1 — 23 May 2026

Priorities are based on user impact relative to implementation effort. The list is intentionally short — only changes worth doing next.

---

## Next priorities

### 1. Body weight log
**Impact: High · Effort: Low**

`BodyWeightEntity` (date + weightKg) already exists in the database. Required work:
- Input UI in the Profile screen or as a card on Home
- Trend chart (Vico, already integrated) covering the last 30 / 90 days
- Optional: surface latest weight on the Home dashboard

This is the highest-return item given the low implementation cost.

---

### 2. Workout history
**Impact: High · Effort: Medium**

The current implementation is a stub. Required work:
- Session detail view (exercises, sets, volume per exercise)
- Volume trend chart by week (Vico)
- Personal records per exercise — `GetPersonalRecordsUseCase` already exists, needs a UI

This closes the feedback loop that motivates consistent training.

---

### 3. Nutrition history day detail
**Impact: Medium · Effort: Low**

The 60-day history screen lists summaries but tapping a row has no effect. Required work:
- Navigate to the nutrition journal for the selected date (date-parameterized navigation already exists)
- Or open a bottom sheet with that day's meal breakdown

Low effort, removes an obvious dead interaction.

---

### 4. Exercise progression chart
**Impact: Medium · Effort: Medium**

In `ExerciseDetailScreen`, add a Vico chart showing estimated 1RM or max weight over the last 30 sessions for that exercise. Requires a query on `WorkoutLogSetEntity` filtered by exercise.

---

### 5. Home screen body weight card
**Impact: Medium · Effort: Low**

Depends on item 1. Once weight logging exists, surface a small card on Home showing the last logged weight and the delta from the previous entry.

---

## Deferred ideas

The following are noted but not prioritized:

- Barcode scanner for food entry (OpenFoodFacts API — requires internet access, significant scope)
- Android home screen widget (Jetpack Glance)
- CSV / Google Sheets export
- Custom program creation (UI partially exists, needs completion)
- Multi-user profiles
