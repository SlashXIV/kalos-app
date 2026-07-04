# Roadmap

> Last updated: v3.17.0 — 3 July 2026
>
> The June 2026 UX review cycle (18 findings, 3 waves: v3.10.1 / v3.11.0 / v3.12.0) is fully closed. No pending UX backlog.

Priorities are based on user impact relative to implementation effort. The list is intentionally short — only changes worth doing next.

---

## Revue générale — 3 July 2026

Revue transverse demandée par l'utilisateur (app jugée globalement complète, focus sur l'amélioration de l'existant). Détail technique des défauts dans `docs/TECHNICAL_AUDIT.md` (section du 3 July 2026).

**Statut : clôturée (v3.18.0 → v3.22.1).** Tout traité sauf : i18n (long terme, décision utilisateur), pagination historique séances (Medium, optionnel), champ `variant`/regroupement catalogue (écarté, sur-ingénierie). Détail par item ci-dessous.

### Corrections (défauts confirmés)

- **Notifications — tap sans effet** — Done v3.18.0. `PendingIntent` vers `MainActivity` + deep-link vers l'écran concerné (nutrition / séance / eau), `launchMode=singleTop`.
- **Notifications — heure non respectée** — Done v3.18.0. `OneTimeWorkRequest` auto-replanifié chaque jour à l'heure cible (remplace le `PeriodicWorkRequest` qui dérivait). Corrige aussi le faux positif « aucun repas loggé » du matin.
- **Notifications — robustesse** — Done v3.22.1. Replanification au lancement (v3.18.0) + log sur permission refusée (v3.22.1).

### Décision produit

- **Retrait des suggestions nutrition** — Done v3.18.0. Carte retirée, `SuggestFoodsUseCase` et `FoodTagger` supprimés. Bonus perf : plus de chargement de toute la table d'aliments dans le combine nutrition.

### Sport — variantes d'exercices

- **Variantes d'un même exercice** (ex. extension triceps corde / barre). Le modèle `Exercise` est plat, les PR/progressions indexés par `exerciseId`.
  - Phase 1 — Done v3.19.0 : convention de nommage « Exercice (attache) », ajout des variantes manquantes courantes (extension triceps barre, tirage prise serrée), phase de seed qui propage les renommages aux installs existants (`updateNameBySeedId`).
  - Phase 2 — évaluée puis **écartée (sur-ingénierie)** lors du lot v3.22.0 : un regroupement fiable des variantes dans le catalogue exigerait un champ clé de groupe/parent curé sur ~150 exercices + une UI dépliable interférant avec le flux d'ajout du builder, pour un bénéfice marginal (la Phase 1 nommage + recherche couvre déjà l'usage). À rouvrir seulement sur signal d'usage réel (catalogue jugé encombré).

### Nouvelles pistes

- **Internationalisation / langues** (Medium-High) : les libellés sont actuellement en dur en français dans le code. Externaliser vers `res/values/strings.xml` (FR par défaut) puis ajouter l'anglais (`values-en`). Gros travail d'extraction mais sans risque. Prérequis à toute ouverture au-delà d'un usage FR.
- **Thèmes multiples** — Done v3.20.0 (thème clair + sélecteur) puis **étendu en v3.25.0** : 4 palettes de couleur complètes (Pastel/Berry/Aurora/Monochrome) en plus d'Émeraude, sélecteur à pastilles (`ThemePalettes.kt`, `colorSchemeFor`). Flash de fond au lancement corrigé en v3.22.1.
- **Performance** (voir TECHNICAL_AUDIT) : N+1 chargement séances/templates — Done (batch `getByIds`). Pagination historique nutrition — Done v3.21.0 (fenêtre 30 j + « Charger plus », `getEarliestMealDate`). Index sur `meal_entry.date` — Done v3.22.0 (migration 16 -> 17). Reste : pagination historique séances (Medium, intriqué avec le graphe de volume).
- **Deep-link notifications** : router chaque notification vers l'écran pertinent (dépend du fix tap-to-open ci-dessus).

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

- ~~Barcode scanner for food entry~~ — Done in v3.14.0. Implémenté en 3 phases par l'audit `docs/SCANNER_AUDIT.md` : data → décodage local (CameraX + ML Kit bundlé) → résolution OpenFoodFacts opportuniste avec cache local. Offline-first préservé (dégradation gracieuse).
- Android home screen widget (Jetpack Glance)
- CSV / Google Sheets export
- ~~Custom program creation~~ — Done in v2.8.0
- ~~Advanced food filters~~ — Done in v2.9.0
- Multi-user profiles

Explicit decisions from the June 2026 UX review (revisit only on real usage signal):

- Drag & drop exercise reorder in the builder — up/down arrows shipped instead (v3.12.0); a drag lib (sh.calvin.reorderable) is the upgrade path if arrows feel limiting
- Full seed content for PPL / Upper-Lower programs — removed from seed instead (v3.12.0); would require ~7 seed templates and would clutter the user's Séances tab
- Undo snackbar or confirmation for journal food deletion — deletion is currently immediate with a muted icon; add a safety net only if accidental deletions actually happen

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

**Statut : volet clos en v3.15.0.** Livré : indice de satiété "rassasiant par calorie" (règle densité + protéines + fibres, label vert/ambre dans les listes et la feuille de portion) + tri "volume eating" dans la recherche. Écartés (faible valeur) : comparaison vs moyenne de catégorie, densité moyenne journalière. Reste possible plus tard : suggestions intelligentes priorisant les aliments peu denses quand le solde calorique est serré (touche `SuggestFoodsUseCase`).

---

### Repas favoris (meal templates)

Beaucoup de repas sont récurrents (ex. salade de thon : quasi toujours les mêmes aliments et grammages). L'objectif : enregistrer un repas type et le réinjecter en un tap, sans re-saisir chaque aliment.

**Statut : volet clos en v3.16.0 (Phase A data + Phase B UI + Phase B2 éditeur).** Livré : tables `meal_template` / `meal_template_item` (migration 15 → 16), enregistrer un repas rempli comme favori (fusion des doublons), appliquer un favori en un tap (aliments ajoutés, jamais de remplacement), écran de gestion (liste + suppression), éditeur complet (renommer, ajuster les grammages, ajouter/retirer des aliments, créer de zéro — via `FoodSearch` réutilisé en mode « pick »), inclusion dans la sauvegarde/restauration JSON. Protection FK RESTRICT sur les aliments référencés par un favori. Éditer/supprimer un favori ne touche jamais les repas déjà journalisés.

---

### Bilan hebdomadaire

Beaucoup de données sont loguées (repas, poids, séances) mais le retour « est-ce que je tiens mon cap ? » était éparpillé (moyenne 7 j dans le calendrier, courbe de poids dans le profil, volume dans l'historique). L'idée : un seul écran de synthèse qui transforme la donnée en feedback.

**Statut : livré en v3.17.0.** Écran `feature/insights` accessible depuis le Profil, périodes 7 j / 30 j. Trois cartes (nutrition / poids / entraînement) + phrase de synthèse « À retenir ». Réutilise `MealRepository.getDailySummaries`, `UserRepository.observeGoal`/`observeProfile`, `WorkoutRepository.getTrainedDates`/`getBodyWeightHistory`, `ProgramRepository.getActive`. **Aucun changement de schéma.** Cible calorique = 90–105 % de l'objectif ; protéines = ≥ objectif ; verdict poids selon `FitnessGoal.kcalDelta`. Écartés (anti-bloat) : ajustement auto de l'objectif (périodisation), export du bilan, détail par aliment, plages arbitraires.
