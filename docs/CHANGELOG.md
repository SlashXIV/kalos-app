# Changelog

---

## v3.1.0 — 25 May 2026

### Added
- Remplacement d'exercice pendant la séance : bouton "Remplacer" par exercice, bottom sheet de sélection (suggestions par groupe musculaire + recherche), confirmation si des séries ont déjà été saisies
- Passage d'exercice : bouton "Passer" par exercice, l'exercice est logué avec `status = SKIPPED` dans l'historique (sans séries)
- Ajout d'exercice hors-programme : bouton `+` en dehors de la `ScrollableTabRow`, exercice inséré avec `status = ADDED`
- Annulation de remplacement : bouton "Annuler le remplacement" disponible tant que la séance est en mémoire (undo non disponible après kill applicatif)
- Badges de statut dans le détail de séance : "Passé" (gris), "Remplacé" (tertiaire, avec nom de l'exercice d'origine), "Hors programme" (vert), "Planifié" sans badge
- DB version 11, `MIGRATION_10_11` : `ALTER TABLE workout_log_exercise ADD COLUMN status TEXT NOT NULL DEFAULT 'PLANNED'` et `ADD COLUMN replacedExerciseName TEXT NOT NULL DEFAULT ''`
- `ExerciseStatus` enum : `PLANNED`, `SKIPPED`, `REPLACED`, `ADDED`

---

## v3.0.0 — 25 May 2026

### Added
- Écran "Mes aliments" : liste de tous les aliments personnalisés, recherche client-side, accès via icône dans la TopAppBar de la recherche d'aliments
- Suppression d'un aliment personnalisé : hard delete si l'aliment n'a jamais été utilisé dans l'historique, soft-delete (`isArchived = true`) s'il a déjà été référencé — les macros dénormalisées dans l'historique restent intactes
- Détection de doublon à la création : vérification par nom normalisé avant enregistrement, dialog d'avertissement avec option "Créer quand même"
- Badge "Perso" sur chaque `FoodListItem` quand `food.isCustom = true`
- Bouton corbeille dans la TopAppBar de `CustomFoodScreen` (mode édition uniquement), avec dialog de confirmation
- Correction : l'édition d'un aliment personnalisé passe désormais le `foodId` existant — évitait la création d'un doublon en base à chaque save
- DB version 10, `MIGRATION_9_10` : `ALTER TABLE food ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0`

---

## v2.9.1 — 23 May 2026

### Changed
- Descriptions explicatives ajoutées à chaque niveau d'activité (`ActivityLevel`) : exemples concrets sous le label dans l'onboarding (`GoalSetupScreen`) et dans la modification de profil (`EditProfileScreen`)
- Aucun changement sur les multiplicateurs TDEE ni sur les calculs

---

## v2.9.0 — 23 May 2026

### Added
- Filtres avancés dans la recherche d'aliments : chips horizontaux défilables sous la barre de recherche
- Chip "Perso" (toggle) : filtre sur les aliments personnalisés uniquement
- Chips de catégorie (single-select) : chargées dynamiquement depuis la DB via `SELECT DISTINCT category`
- Filtres combinables entre eux et avec la requête texte
- Filtre actif sans texte → affiche le catalogue filtré au lieu de Récents/Favoris

---

## v2.8.0 — 23 May 2026

### Added
- Programmes personnalisés : éditeur complet (`ProgramEditorScreen`) avec nom, description, durée en semaines, et assignation de séances par jour (Lun–Dim) via sélecteur bottom sheet
- FAB "+" sur l'écran Programmes (onglet Sport et écran standalone) pour créer un nouveau programme
- Icône crayon sur les cartes de programmes personnalisés pour accéder à l'édition
- Suppression d'un programme personnalisé depuis l'éditeur (bouton corbeille, confirmation requise)
- `isCustom` ajouté sur la table `training_program` (DB version 9, migration 8→9 non destructive)

---

## v2.7.0 — 23 May 2026

### Added
- Exercices favoris dans le catalogue : icône cœur en trailing sur chaque item (mode standalone uniquement), toggle persisté en base
- Chip "Favoris" en tête des filtres du catalogue, combinable avec les filtres muscle et type
- `isFavorite` column ajoutée sur la table `exercise` (DB version 8)

---

## v2.6.0 — 23 May 2026

### Added
- Volume trend chart dans `WorkoutHistoryScreen` : bar chart Canvas, agrégation hebdomadaire (somme des `totalVolumeKg` par semaine ISO), 8 dernières semaines
- Barres arrondies, 3 lignes de grille, labels de date (d MMM) sur première, dernière et semaine centrale
- Ligne récapitulative : volume total sur la période affichée
- Affiché uniquement si ≥ 2 semaines de données non nulles

---

## v2.5.0 — 23 May 2026

### Added
- Body weight card dans l'écran **Profil** : dernier poids enregistré, date (Aujourd'hui / Hier / d MMM), delta vs entrée précédente avec signe, tap vers Suivi du poids
- Carte absente si aucune pesée enregistrée

### Removed
- `HomeScreen` et `HomeViewModel` supprimés — écran jamais câblé dans la navigation, vestige du plan initial
- Section "Home" retirée de la documentation (les features décrites étaient portées par l'écran mort)

---

## v2.4.0 — 23 May 2026

### Added
- Exercise progression chart in `ExerciseDetailScreen`: Canvas line chart showing max weight per session over the last 20 sessions
- X-axis uses real dates (not session index) — first and last date labeled
- PR badge ("PR : X.X kg") displayed in the card header in green
- Chart appears only from 2 sessions; single session shows a placeholder message; 0 sessions hides the section

---

## v2.3.0 — 23 May 2026

### Added
- Nutrition history: tapping a day in the 60-day list opens the journal for that date
- Dates in the history list are now formatted (Aujourd'hui / Hier / Lundi 19 mai)
- When opened from history: back button in the top bar, history icon hidden
- Journal is editable for past days: add/remove food entries and water intake on any date
- Water goal modification remains today-only; smart suggestions remain today-only
- TopAppBar title shows the date whenever a past day is active (history navigation or arrow navigation from main tab)

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
