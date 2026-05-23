# Roadmap

> Last updated: v2.3.0 — 23 May 2026

Priorities are based on user impact relative to implementation effort. The list is intentionally short — only changes worth doing next.

---

## Next priorities

### ~~1. Body weight log~~ — Done in v2.1.2

---

### ~~1. Workout history~~ — Done in v2.2.0

Session detail view, tappable history list, and PR per exercise delivered. Volume/progression charts explicitly deferred.

---

### ~~1. Nutrition history day detail~~ — Done in v2.3.0

Tapping a row in the 60-day history now opens the nutrition journal for that date in read-only mode.

---

### ~~2. Home screen body weight card~~ — Done in v2.5.0

Last weight, date, delta vs previous entry. Card hidden when no entry yet.

---

### ~~3. Exercise progression chart~~ — Done in v2.4.0

Canvas line chart in `ExerciseDetailScreen`: max weight per session (last 20), date real X-axis, PR badge. Chart shown from 2 sessions.

---

### 4. Workout volume trend chart
**Impact: Medium · Effort: Medium**

Weekly volume aggregation (sum of reps × weight per week) shown in WorkoutHistoryScreen or a dedicated insights panel. Deferred from workout history v1 — natural follow-up once the detail screen is validated.

---

## Deferred ideas

The following are noted but not prioritized:

- Barcode scanner for food entry (OpenFoodFacts API — requires internet access, significant scope)
- Android home screen widget (Jetpack Glance)
- CSV / Google Sheets export
- Custom program creation (UI partially exists, needs completion)
- Multi-user profiles
