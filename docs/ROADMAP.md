# Roadmap

> Last updated: v4.3.2 — 25 October 2026
>
> The June 2026 UX review cycle (18 findings, 3 waves: v3.10.1 / v3.11.0 / v3.12.0) is fully closed. No pending UX backlog.

Priorities are based on user impact relative to implementation effort. The list is intentionally short — only changes worth doing next.

---

## Refonte graphique « Graduation » — octobre 2026

Sortir Kalos de son allure « Material par défaut » avec la méthode mise au point sur Dromos (métaphore unique, planche validée avant le code, système maison, palettes en registre testées, mouvement à niveaux, moments, accessibilité et performance mesurées), sans reprendre sa direction artistique.

**Planche validée le 13 octobre 2026** : https://claude.ai/code/artifact/83c6ed52-5897-44bf-aba9-105799ed396c

**Métaphore unique : la graduation.** Tout ce que Kalos compte se mesure (kcal, grammes, ml, kg, charges, secondes) : règles graduées, aiguille qui se pose avec un amorti, cadran gradué à la place de l'anneau, repas en tickets de pesée, objectif = une zone, graduations qui s'allument quand il est atteint.

| Jalon | Version | Statut | Preuve |
|---|---|---|---|
| R1 — Fondations | v4.0.0, v4.3.0 | Done | 28 tests JVM verts (contrastes et daltonisme des 3 palettes × clair/sombre, registre, migration des thèmes 3.x, niveau de mouvement, textes en dur, calculs) ; lint sans nouvel avertissement ; tous les écrans vérifiés sur émulateur en sombre et clair, police 200 %, densités 520 et 600, paysage ; arbre TalkBack (uiautomator) : une phrase par mesure ; CPU au repos sur Aujourd'hui ≈ 0,02 s/min, scanner 57,5 s/min (identique à 3.26.2 : le viseur ne coûte rien) ; téléphone 2506BPN68G, build optimisé : défilement du catalogue (160 exercices) et de la recherche d'aliments 0 à 0,06 % d'images en retard, médiane 5 ms, 99e centile 6 ms ; CPU au repos sur Aujourd'hui 0,00 s/min |
| R2 — Moments | v4.2.0 | Done | Quatre moments vérifiés sur émulateur, animations ralenties ×5 et captures : pesée de 0 à 712 kcal, objectif atteint (graduations allumées, statut « Objectif du jour atteint », pas de reprise au relancement), tampon et annonces « Série 1 validée. Repos, 90 secondes » / « Repos terminé » (absente quand le repos est passé), récapitulatif (titre, volume compté de 702 à 1 230 kg, règle remplie). Règle de zone testée (JVM) |
| R3 — Vitrine (README, kit portfolio) | v4.0.1 | Done | Icône vectorielle `design/icon/kalos-icon.svg` comparée côte à côte avec Brut, Dromos et Topo ; vérifiée sur émulateur (tiroir d'applis, écran de lancement) ; README avec quatre captures `design/screenshots/` |

### R1 — Fondations (v4.0.0) — High

- **Polices embarquées (OFL)** — Done. Big Shoulders Display (affiche), Hanken Grotesk (texte), Martian Mono condensée (chiffres à chasse fixe). Licences dans `assets/licenses/`.
- **Styles nommés par usage** — Done. `KalosType` : `poster`, `posterSmall`, `metricHuge`, `metricLarge`, `metric`, `metricSmall`, `label`, `action`.
- **Formes en jetons** — Done. `KalosShape` : `chip` 4, `control` 6, `card` 10, `sheet` 14 dp.
- **Couleurs des nutriments hors Material** — Done. `NutrientColors` (protéines, glucides, lipides, eau, objectif atteint, au-dessus), identiques dans toutes les palettes.
- **Palettes en registre + test** — Done. Jade (par défaut), Encre, Océan, en clair et en sombre ; neutres communs. Pastel, Berry, Aurora, Aqua, Sunset retirés (repli sur Jade en gardant la luminosité). `PaletteContrastTest`.
- **Mouvement** — Done. `Motion` (pesée, entrée, transition), `MotionLevel` FULL / REDUCED / NONE, réglage « Ambiance » (Expressive / Sobre) ; « Supprimer les animations » relu à chaque retour dans l'appli. `Haptics` centralisée, coupée par le réglage « Vibrations ».
- **Textes dans strings.xml** — Done. `HardcodedUiTextTest` ; la liste d'attente `hardcoded-ui-pending.txt` est vide.
- **Composants maison** — Done. `FitText`, `RisingText`, `CountUp`, `RollingDigits`, `GraduatedScale`, `GraduatedDial`, `Graduation`, `MeasureRow`, `MeasureTicket`, `ScreenHeader`, `KalosButton`, `ValueChip`, `KalosFilterChip`, `MeSeriesChart`.
- **Tous les écrans dans le nouveau langage, clair et sombre** — Done. Aujourd'hui (ex-Nutrition), nutrition, sport, calendrier, bilan, profil, réglages. L'onboarding, jamais branché, est supprimé (et avec lui la route `home` morte).
- **Graphiques refaits (méthode dataviz)** — Done. Bilan et poids (v4.0.0), progression d'exercice et volume hebdomadaire (v4.3.0) sur `MeSeriesChart` : curseur au doigt, au clavier et TalkBack ; Vico retiré. Vérifiés sur émulateur avec une vraie sauvegarde importée, clair et sombre ; semaines sans séance à zéro (test JVM).
- **Accessibilité et performance mesurées** — Done. Accessibilité vérifiée (voir Preuve) ; `FitText` réduit d'après la taille réellement dessinée. Performance : mesurée sur téléphone (voir Preuve) ; sur émulateur, seuls le CPU au repos et le scanner sont exploitables (défilement trop bruité : 22 à 80 % d'images en retard d'une série à l'autre sur le même APK). Minuteur de repos : à mesurer avec les moments (R2).

**Critère d'acceptation.** Aucun écran ne « fait Material par défaut » ; Sobre et « Supprimer les animations » coupent tout mouvement décoratif ; tests JVM verts ; aucun nouvel avertissement lint.

### R2 — Moments (v4.2.0) — High — Done

Aliment ajouté (l'aiguille avance), objectif du jour atteint (les graduations s'allument, une fois par jour), série validée et repos (cadran qui se vide, vibration en fin de repos), séance terminée (récapitulatif). Interruptibles d'un toucher, haptique centralisée, annonces TalkBack au début et à la fin seulement.

### R3 — Vitrine — Medium

- **Icône dans la grammaire commune** — Done (v4.0.1). K dessiné, règle graduée verte, anneau des macros ; source SVG, calques vectoriels, icône à thème.
- **README illustré** — Done (v4.0.1). Icône, accroche et quatre captures sur émulateur (profil de démonstration, aucune donnée réelle).
- **Kit portfolio** — Done (25 octobre 2026, hors dépôt : `portfolio/web`). Fiche réécrite pour la v4 (lien APK), démo jouable refaite dans « la graduation » : cadran et règles, aliments à peser, objectif calculé par les formules de l'app, moment « objectif atteint », Jade / Encre / Océan en clair et sombre ; aperçu 1000 × 625 capturé sur la démo.

### Journal des décisions (refonte)

| Décision | Raison |
|---|---|
| Métaphore unique « la graduation » | la cohérence d'une métaphore, pas la quantité d'effets, fait l'identité |
| Trois palettes (Jade, Encre, Océan) en registre + test | lisibilité vérifiée à chaque build ; Prune écartée, trop proche du rose des lipides |
| Pas de couleurs dynamiques (Material You) | contrastes choisis et testés, pas hérités du fond d'écran |
| Français seul, textes dans strings.xml | remplace la décision « i18n long terme » du 4 juillet ; préalable à toute autre langue |
| Polices embarquées (OFL), variables | hors-ligne ; une graisse = un réglage d'axe |
| Réglage « Ambiance » + respect de « Supprimer les animations » | le mouvement ne doit jamais coûter la lisibilité ni le confort |
| Contrôle des textes en dur par test JVM plutôt que tâche Gradle | Kalos n'a qu'un module : pas de `buildSrc` à maintenir, et le détecteur est testé |
| Écrans migrés en trois familles parallèles, puis relecture indépendante | la relecture n'a trouvé aucune régression de comportement, et quelques défauts d'accessibilité corrigés avant la release |
| Performance de défilement mesurée sur téléphone | l'émulateur (rendu logiciel partagé avec les builds) donne des écarts plus grands que l'effet mesuré |
| Avertissements lint antérieurs figés dans `lint-baseline.xml` | tout nouvel avertissement se voit ; les 90 mises à jour de dépendances restent une décision à part |

### Hors refonte, relevé au passage

- `CalculateMacroGoalsUseCaseTest` attendait 2 g/kg de protéines alors que le calcul applique le ratio de chaque objectif depuis la transparence des macros : test aligné sur le calcul (décision de Gabriel), calcul inchangé. — Done v4.0.0
- `versionName` / `versionCode` alignés sur le CHANGELOG : 4.0.0 / 40000 (décision de Gabriel). — Done v4.0.0
- Onboarding jamais branché à la navigation : supprimé (décision de Gabriel). — Done v4.0.0
- Défauts relevés à la relecture, non bloquants (Low) : libellés TalkBack qui gardent l'unité au pluriel pour une quantité de 1 (« 1 grammes ») ; bouton Enregistrer sans indicateur pendant la sauvegarde (aliment perso, objectifs) ; libellés de date « Aujourd'hui / Hier » mis en forme à cinq endroits ; `formatElapsedSince` encore en dur dans le code. — Planned

---

## Audit technique — 25 October 2026

Détail et statuts dans `docs/TECHNICAL_AUDIT.md` (A1 à A20).

| Lot | Version | Statut | Priorité |
|---|---|---|---|
| Pertes de données et saisie (A1 à A6, A8, A9) | v4.0.2 | Done | High |
| Tests de migration 13→17 (A7) | v4.0.2 | Done | High |
| Migration 17→18 : unicité et index, codes stables pour les exercices (A12, A13) | v4.1.0 | Done | Medium |
| Séance active : écriture du brouillon, recompositions (A10, A11) | v4.1.0 | Done | Medium |
| Règles métier centralisées et testées, date du jour suivie (A14, A16, A19) | v4.1.0 | Done | Medium |
| Filtres alimentaires rebranchés sur la recherche (A18) | v4.1.0 | Done | Medium |
| Activités typées avec dépense estimée, affichée seulement (O1) | v4.1.0 | Done | Low |
| e1RM et record par répétitions, saisie du RPE, affichage en livres (O2) | — | Planned (décision produit) | Low |
| Programmes datés : semaine courante, un seul programme actif garanti (O3) | — | Planned (décision produit) | Low |
| Profil de référence (baseline profile) : module de benchmark à créer | — | Planned | Low |
| Recette release avec vraie sauvegarde : historique et moyenne après une pause (A22) | v4.3.1 | Done | High |
| Pages de 16 Ko : CameraX 1.3.4 → 1.4.2 (A23) | v4.3.2 | Done | Medium |

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
