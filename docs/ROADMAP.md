# Roadmap

> Last updated: v3.5.0 — 27 May 2026

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

### ~~4. Workout volume trend chart~~ — Done in v2.6.0

Weekly aggregation of `totalVolumeKg`, last 8 weeks, bar chart in `WorkoutHistoryScreen`.

---

## Deferred ideas

The following are noted but not prioritized:

- Barcode scanner for food entry (OpenFoodFacts API — requires internet access, significant scope)
- Android home screen widget (Jetpack Glance)
- CSV / Google Sheets export
- ~~Custom program creation~~ — Done in v2.8.0
- ~~Advanced food filters~~ — Done in v2.9.0
- Multi-user profiles

---

### Densité calorique & aide au volume eating

**Contexte produit :**
En phase de sèche, le budget calorique est contraint mais la faim ne l'est pas. Certains aliments sont très denses (ex. oléagineux : 600 kcal/100 g) et peu rassasiants, d'autres offrent un volume important pour peu de calories (ex. concombre : 15 kcal/100 g, blanc d'œuf : 52 kcal/100 g). L'idée est d'aider l'utilisateur à maximiser la satiété dans son enveloppe calorique.

**Données déjà disponibles :**
`kcalPer100g`, `proteinPer100g`, `fiberPer100g` sont présents sur tous les aliments. La densité brute est calculable immédiatement (`kcalPer100g / 100`). La satiété est une heuristique plus complexe (protéines + fibres + densité inverse).

**Pistes d'implémentation :**

- **Signal de densité** : badge coloré sur `FoodListItem` (vert < 150 kcal/100 g, orange 150–350, rouge > 350) — visible dans la recherche et les favoris
- **Tri par densité** : option de tri dans `FoodSearchScreen` ("Volume eating" = tri croissant densité)
- **Indice de satiété simplifié** : score composite = protéines × 2 + fibres × 3 − densité (normalisé), affiché dans `FoodDetailSheet`
- **Comparaison visuelle** : dans `FoodDetailSheet`, une barre "volume pour 100 kcal" comparée à la moyenne de la catégorie
- **Suggestions volume eating** : `SmartSuggestionEngine` peut prioriser les aliments faibles en densité quand le solde calorique est serré (< 500 kcal restants)
- **Indicateur journalier** : densité moyenne de la journée dans le header du journal nutritionnel

**Effort estimé :** signal de densité seul = 1 jour ; indice de satiété + comparaison = 2–3 jours ; suggestions intelligentes = 1 jour supplémentaire.

**Prérequis :** aucun changement de schéma DB nécessaire pour le signal de densité et le tri — les données sont déjà là.
