# Product Audit

> Version: 2.1.1 — 23 May 2026

---

## Overall assessment

Kalos is a feature-complete, offline-first fitness and nutrition tracker. The core flows — onboarding, daily meal logging, and active workout tracking — are solid and well-polished. Two secondary modules (workout history, custom programs) remain incomplete. The main structural gap is the absence of a body weight log UI despite the database entity already existing.

---

## Module-by-module review

### Onboarding

**Solid.** Four-step flow is complete and coherent. TDEE and macro calculation (Mifflin-St Jeor) is correctly implemented and explained to the user on the result screen. No re-onboarding mechanism from settings, but all fields are editable from the profile screen so this is non-blocking.

### Home

**Solid.** The dashboard is contextual and reactive. The program card correctly distinguishes between a scheduled workout day and a rest day, and displays the next planned session. Data updates in real time via StateFlow.

### Nutrition

**Most polished module.** The daily journal covers the full logging loop: meal sections, hydration with quick-add buttons and negative corrections, smart suggestions based on remaining macros and dietary filters, and a projected macro strip in the food detail sheet. Dietary filter logic is consistent across search results and suggestions.

The only notable gap: the 60-day history screen lists daily summaries but a tap on a row has no action. The detail view (what was actually eaten that day) is missing.

### Workout

**Core flow complete; secondary features incomplete.**

The builder → active tracker → summary path works well. The draft auto-save and resume dialog (with staleness detection beyond 24 h) are particularly solid. Total volume is calculated and persisted correctly.

Gaps:
- **Workout history** is a stub. The list of past sessions is displayed but there is no interaction, no session detail, and no progression charts. This is the most visible gap for a regular user.
- **Custom programs** creation and editing are partially implemented.
- **Body weight log** has a database entity (`BodyWeightEntity`) but no UI at any level (no input, no chart, no home card). This is a significant omission for a fitness tracker.

### Calendar

**Complete.** Monthly grid with three-state day indicators (nutrition logged, workout done, planned), animated day detail card, and two insights (4-week workout frequency bar chart, 7-day average calorie). The "Cette sem." label clipping issue was resolved in v2.1.0.

### Profile

**Complete.** Profile and goal editing both handle recalculation correctly. The goal edit screen shows a detailed breakdown before saving, which is good for user understanding. No body weight tracking UI (see Workout section above).

### Settings

**Complete.** Dietary preferences integrate across the full nutrition flow. Export and import work with a proper confirmation dialog on import. The notifications link navigates to the dedicated screen.

### Notifications

**Complete.** Smart reminders (WorkManager daily job) and program reminders are both functional, configurable, and centralized in a dedicated screen since v2.1.0. Per-program toggles, inactivity threshold chips (2 / 3 / 5 days), and time sliders are all correctly wired.

---

## Identified gaps (prioritized)

| Priority | Area | Description |
|---|---|---|
| High | Body weight log | `BodyWeightEntity` exists in the database with no UI. Weight input, trend chart, and integration with the Home dashboard are all missing. |
| High | Workout history | Current implementation is a stub. No session detail, no volume trend, no personal records display (use case already exists: `GetPersonalRecordsUseCase`). |
| Medium | Nutrition history detail | Tapping a day in the 60-day history has no action. Expected behavior: navigate to that day's journal or show a summary sheet. |
| Medium | Custom programs | Creation and editing UI are incomplete. |
| Low | Exercise favorites | The catalog has no way to mark or filter favorite exercises. |
| Low | Advanced food filters | Food search has no category or tag filter, only a text query. |

---

## UX notes

- Color system is consistent since v2.1.1 (secondary token remapped to green, no residual brown).
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
