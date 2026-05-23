# Product Audit

> Version: 2.7.0 — 23 May 2026

---

## Overall assessment

Kalos is a feature-complete, offline-first fitness and nutrition tracker. The core flows — onboarding, daily meal logging, and active workout tracking — are solid and well-polished. Body weight log, workout history, nutrition history, and exercise progression chart are complete. The remaining gaps are Low priority.

---

## Module-by-module review

### Onboarding

**Solid.** Four-step flow is complete and coherent. TDEE and macro calculation (Mifflin-St Jeor) is correctly implemented and explained to the user on the result screen. No re-onboarding mechanism from settings, but all fields are editable from the profile screen so this is non-blocking.

### Nutrition

**Most polished module.** The daily journal covers the full logging loop: meal sections, hydration with quick-add buttons and negative corrections, smart suggestions based on remaining macros and dietary filters, and a projected macro strip in the food detail sheet. Dietary filter logic is consistent across search results and suggestions.

The only notable gap: the 60-day history screen lists daily summaries but a tap on a row has no action. The detail view (what was actually eaten that day) is missing.

### Workout

**Core flow complete; secondary features incomplete.**

The builder → active tracker → summary path works well. The draft auto-save and resume dialog (with staleness detection beyond 24 h) are particularly solid. Total volume is calculated and persisted correctly.

Gaps:
- **Custom programs** creation and editing are partially implemented.
- **Home body weight card** not yet surfaced on the dashboard.

### Calendar

**Complete.** Monthly grid with three-state day indicators (nutrition logged, workout done, planned), animated day detail card, and two insights (4-week workout frequency bar chart, 7-day average calorie). The "Cette sem." label clipping issue was resolved in v2.1.0.

### Profile

**Complete.** Profile and goal editing both handle recalculation correctly. Body weight card on this screen shows last logged weight, date, and delta vs previous entry — tappable to the full weight log.

### Settings

**Complete.** Dietary preferences integrate across the full nutrition flow. Export and import work with a proper confirmation dialog on import. The notifications link navigates to the dedicated screen.

### Notifications

**Complete.** Smart reminders (WorkManager daily job) and program reminders are both functional, configurable, and centralized in a dedicated screen since v2.1.0. Per-program toggles, inactivity threshold chips (2 / 3 / 5 days), and time sliders are all correctly wired.

---

## Identified gaps (prioritized)

| Priority | Area | Description |
|---|---|---|
| ~~High~~ | ~~Body weight log~~ | Done in v2.1.2 |
| ~~High~~ | ~~Workout history~~ | Done in v2.2.0 — list, detail, PRs |
| ~~Medium~~ | ~~Nutrition history detail~~ | Done in v2.3.0 |
| ~~Medium~~ | ~~Exercise progression chart~~ | Done in v2.4.0 — Canvas chart, date X-axis, PR badge |
| ~~Medium~~ | ~~Home body weight card~~ | Done in v2.5.0 — last weight, date, delta |
| Medium | Custom programs | Creation and editing UI are incomplete. |
| ~~Low~~ | ~~Exercise favorites~~ | Done in v2.7.0 — heart icon toggle + Favoris filter chip |
| Low | Advanced food filters | Food search has no category or tag filter, only a text query. |

---

## UX notes

- Color system is fully consistent since v2.2.0 — `surfaceContainerLow` and full container family now defined, eliminating gray fallback on `ElevatedCard`.
- French decimal separator (`,`) is handled correctly in numeric inputs.
- The food projection strip ("Après ajout") is a strong UX decision — visible before committing.
- No user avatar or photo support (not a stated requirement, non-blocking).
- The TDEE displayed in the profile is a static calculation from the last saved profile. It is not invalidated automatically when macros are manually edited.

---

## Technical notes

| Area | Status |
|---|---|
| Architecture (MVVM + Repository + UseCase) | Clean, well-separated |
| Reactive state (StateFlow + combine) | Correctly used throughout |
| Dependency injection (Hilt) | Consistent |
| Database migrations | `fallbackToDestructiveMigration` — acceptable for current stage |
| Notifications | WorkManager + EntryPoint pattern (not @HiltWorker) |
| Compose performance | `remember(key)` in place for CalendarGrid; general performance acceptable |
| Theme | Material3 dark, all tokens correctly mapped |
