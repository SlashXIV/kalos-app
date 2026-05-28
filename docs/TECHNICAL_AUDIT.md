# Technical Audit — Kalos

> Date initial : 28 May 2026
> Version auditée : 3.6.0
> Sprint critique appliqué dans v3.7.0 — items marqués `[FIXED v3.7.0]` ci-dessous
> Périmètre : code Kotlin (app/src/main), schéma DB, export/import, navigation, documentation `docs/`
> Méthode : lecture directe + exploration ciblée par agents, vérification croisée des claims contre le code

---

## Résumé exécutif

Kalos est dans un état **fonctionnel et globalement sain** pour une app solo offline-first à ce niveau de maturité (60 jours, 3.6.0). L'architecture MVVM + Repository + UseCase est correctement appliquée, la réactivité (StateFlow + combine) est cohérente, et les flux principaux (onboarding, journal, séance active) sont solides.

Les vrais risques se concentrent sur **trois zones** :

1. **Robustesse de la persistance** — combo `fallbackToDestructiveMigration()` + `exportSchema = false` + absence de tests de migration. Une migration ratée wipe les données utilisateur sans alerte.
2. **Atomicité des opérations multi-étapes** — `finish()` séance, `saveSetEdit`, et `BackupImporter.import()` enchaînent plusieurs writes Room sans transaction. Un crash en cours laisse la DB partiellement modifiée.
3. **Absence quasi-totale de gestion d'erreurs au niveau ViewModel** — un seul fichier sur tout `feature/` contient un `try/catch` ou `runCatching`. Toutes les opérations DB côté UI sont aveugles : si Room throw, la coroutine meurt en silence et l'UI reste figée sur un spinner ou affiche un faux succès.

Aucun de ces points n'est bloquant pour l'usage actuel, mais ils méritent une vague de durcissement avant la prochaine itération produit majeure.

**Verdict global** : code bien tenu, dette technique très contenue, deux ou trois patterns à corriger systématiquement, et un travail spécifique de robustesse sur backup + migrations.

---

## Points positifs (à conserver tels quels)

| Sujet | Qualité |
|---|---|
| Séparation des couches | Stricte. Entity / DAO / Repository / UseCase / VM / Screen. Pas de fuite cross-couche. |
| Réactivité Compose | `collectAsStateWithLifecycle()` partout, `StateFlow.stateIn(SharingStarted.WhileSubscribed(5000))` standardisé. |
| Hilt | Modules clean (`DatabaseModule`, `RepositoryModule`, `UseCaseModule`), pas de @Inject lateinit hors Application. |
| Mappers Entity ↔ Domain | Centralisés dans `core/data/mapper/`, pas de leak d'entity dans les ViewModels. |
| Snapshot vs dynamique nutrition | Décision produit cohérente : macros snapshotées dans `MealEntryItem`, nom résolu via FK → l'historique reste numériquement fidèle même si une fiche est modifiée. |
| Backup ID remapping | Les ID auto-incrémentés sont correctement remappés à l'import via `foodIdMap` / `templateIdMap`. L'ordre d'insertion respecte les FK. |
| Seed différentiel exercices (v3.2.0) | Pattern propre : phase backfill `seedId` + phase insert. Les exercices custom ne sont jamais touchés. |
| Auto-save draft séance active | Debounce 400 ms + `ignoreUnknownKeys` à la désérialisation = tolérance aux ajouts de champs. |
| Décisions FK | CASCADE pour les enfants strictement composites (sets→exercise→log), RESTRICT pour FoodEntity → bonne intention produit (ne pas perdre l'historique nutritionnel si une fiche disparaît). |

---

## Findings critiques (à corriger en priorité)

### C1. `fallbackToDestructiveMigration()` + `exportSchema = false` `[FIXED v3.7.0]`

**Fichier** : `core/database/KalosDatabase.kt:31`, `di/DatabaseModule.kt:24`

`Room.databaseBuilder(...).fallbackToDestructiveMigration()` : si Room ne trouve pas de migration pour une transition de versions (par exemple un downgrade en debug, ou une migration qui throw), la DB est **wipée intégralement sans confirmation utilisateur**. Combiné à `exportSchema = false`, on n'a aucun moyen de tester en CI qu'une migration produit bien le schéma attendu.

**Impact concret** : un utilisateur qui installe une build récente puis revient à une plus ancienne (ou un bug de migration en prod) perd toutes ses données. Aucune trace, aucun warning.

**Recommandation** :
- Retirer `fallbackToDestructiveMigration()`. Une migration manquante doit échouer bruyamment, pas wiper.
- Mettre `exportSchema = true`, ajouter `app/schemas/` à git, et écrire au moins un `MigrationTestHelper` Android-test pour les transitions récentes.
- Si on veut garder un filet de sécurité, utiliser `fallbackToDestructiveMigrationOnDowngrade()` qui ne déclenche que sur downgrade.

**Effort** : 3 h pour la config + 4 h pour un premier test de migration paramétré v11→v12→v13.

---

### C2. Aucune transaction sur les opérations multi-writes `[PARTIAL — b FIXED v3.7.0]`

Trois zones enchaînent plusieurs writes sans `@Transaction` ni `database.withTransaction { }` :

**a) `ActiveWorkoutViewModel.finish()` (lignes 419-471)**
- Crée le log (`startLog`)
- Pour chaque exercice complété, appelle `upsertSet` N fois
- Termine avec `finishLog(logId, durationSecs, totalVolume)`

Si l'appli est tuée pendant la boucle des sets, le log existe en base mais incomplet ; au prochain démarrage il apparaît dans l'historique avec un sous-ensemble de sets et un volume = 0.

**b) `BackupImporter.import()` (lignes 60-224)**
- Vide TOUT (`clearMealEntries`, `clearWorkoutLogs`, etc.) **en premier**
- Puis ré-insère sur ~150 lignes de coroutines suspendues

Si l'import échoue à la ligne 130 (par ex. un `foodId` orphelin viole la FK RESTRICT), **toutes les données précédentes ont déjà été supprimées** et seule une partie est restaurée. Pas de rollback, pas de warning, l'utilisateur se retrouve avec une app à moitié vide.

**c) `WorkoutSummaryViewModel.saveSetEdit` + `WorkoutLogDetailViewModel.saveSetEdit`** (les deux écrans)
- `upsertSet(...)`
- `getLog(...)` (lecture)
- `finishLog(logId, duration, newVolume)`

Si un autre thread modifie un set entre la lecture et le `finishLog`, le volume écrit est obsolète.

**Impact concret** : data partielle, état incohérent, et dans le cas (b) **risque de perte totale** des données utilisateur si le format d'import a un défaut.

**Recommandation** :
- Ajouter `database.withTransaction { ... }` dans `BackupImporter.import()` (priorité #1 absolue).
- Encapsuler `finish()` dans une `@Transaction suspend fun completeWorkout(...)` au niveau du DAO ou repository.
- Idem pour `saveSetEdit`.

**Effort** : 4 h (incluant tests manuels d'import partiel pour vérifier le rollback).

---

### C3. Aucun `try/catch` dans les ViewModels `[FIXED v3.7.0 sur les 4 VMs critiques]`

`grep -r "runCatching\|try {\|catch (" app/src/main/java/com/kalos/app/feature/` ne renvoie qu'**un seul fichier** : `ActiveWorkoutViewModel.kt` (pour la désérialisation du draft). Tous les autres VMs lancent leurs `viewModelScope.launch { repo.foo(); _state.update { ... } }` à nu.

Exemples flagrants :
- `CustomFoodViewModel.doSave()` : `foodRepository.save(...)` peut throw (FK constraint, DB lock). Le `_state.update { it.copy(savedSuccessfully = true) }` à la ligne 144 s'exécute **après** une éventuelle exception → si Room throw, le composable LaunchedEffect ne déclenche pas la navigation et l'utilisateur reste bloqué sur un spinner sans message.
- `FoodSearchViewModel.addToMeal()` : aucune trace si `getOrCreateMealEntry` ou `addItemToMeal` échoue, mais `addedSuccessfully = true` est posé en sortie.
- `ExportViewModel` / `ImportViewModel` : pas vérifiés en détail mais probablement même pattern, le seul `Result<Unit>` retourné par `BackupImporter.import()` est tributaire des `runCatching` internes.

**Impact concret** : faux succès silencieux, UI figée, et impossibilité de remonter un crash analytique.

**Recommandation** :
- Introduire un pattern unique `viewModelScope.launch { runCatching { ... }.onSuccess { ... }.onFailure { _state.update { it.copy(error = ...) } } }`.
- Au minimum sur tous les save/delete/import/export.
- Afficher l'erreur dans un snackbar via un `errorMessage: String?` dans chaque UiState.

**Effort** : 6-8 h pour passer sur tous les ViewModels critiques (Custom food, Profile, Export, Import, Workout finish, Edit set ×2).

---

### C4. Import orphelins → crash + DB déjà purgée `[FIXED v3.7.0]`

**Fichier** : `core/export/BackupImporter.kt:129` et `:178` et `:215`

```kotlin
val resolvedFoodId = foodIdMap[item.foodId] ?: item.foodId
```

Si un `foodId` référencé par un meal item est absent du backup (cas réaliste : aliment supprimé avant l'export), le code retombe sur l'ID original. Si cet ID ne correspond plus à aucun aliment seed/custom dans l'install cible → `SQLiteConstraintException` (FK RESTRICT) → exception. Comme noté en C2, la purge ayant déjà eu lieu, **toutes les données précédentes sont perdues**.

Même problème sur `templateId` et `resolvedTemplateId`.

**Recommandation** :
1. Avant la purge, valider que tous les `foodId` / `templateId` référencés dans le backup existent (en seed ou dans le backup lui-même). Échouer **avant** d'avoir touché à quoi que ce soit.
2. Pour les orphelins acceptables (aliment seed supprimé entre deux builds), logger et sauter l'item plutôt que de crasher.

**Effort** : 3 h pour la phase de validation + 1 h de test.

---

## Findings hautes (à programmer dans les 2-3 prochaines releases)

### H1. Duplication massive entre `WorkoutSummaryViewModel` et `WorkoutLogDetailViewModel`

**Fichiers** : `feature/workout/active/WorkoutSummaryScreen.kt:46-89` et `feature/workout/history/WorkoutLogDetailScreen.kt:56-115`

Les deux écrans portent désormais la même feature "édition de série" avec :
- `SummaryPendingEdit` ↔ `PendingSetEdit` (data classes quasi identiques)
- `saveSetEdit()` quasi identique (recharge + recalcul volume + finishLog)
- `SummaryEditSetDialog` ↔ `EditSetDialog` (~50 lignes dupliquées chacune)
- Extensions `Float.toWeightInput()` dupliquées (signatures privées identiques dans les deux fichiers)

Le ViewModel `WorkoutLogDetailViewModel` calcule en plus les `maxWeights` par exercice (lignes 64-80) avec un pattern N+1 (`getMaxWeight(exerciseId)` pour chaque exercice).

**Impact** :
- Tout bug fix doit être appliqué dans deux fichiers (haut risque d'oubli).
- Le N+1 sur maxWeights devient sensible sur des logs avec 15-20 exercices.

**Recommandation** :
- Extraire un composable partagé `EditableSetRow(set, onEdit)` et un dialog partagé dans `core/ui/component/` ou un fichier sibling.
- Extraire `saveSetEdit` dans `WorkoutRepository.editSet(logId, exerciseId, set)` (et le rendre transactionnel — cf. C2).
- Remplacer le N+1 par une requête Room unique `getMaxWeightsForExercises(ids: List<Long>): Map<Long, Float?>`.

**Effort** : 3-4 h.

---

### H2. `lastUsedAt` non backupé `[FIXED v3.7.0]`

**Fichier** : `core/export/KalosBackup.kt` (FoodBackup) + `BackupImporter.kt:105-118`

`FoodEntity.lastUsedAt` n'est pas dans `FoodBackup`. À l'import, le champ reste à `0` (default). Conséquence : après restauration, la section "Récents" du `FoodSearchScreen` est vide jusqu'à ce que l'utilisateur reconsomme chaque aliment.

**Impact** : Dégradation visible de l'UX après restauration. Pas une perte définitive mais frustrante.

**Recommandation** : Ajouter `lastUsedAt: Long = 0L` à `FoodBackup`, mapper côté export et import. Pas de migration backup nécessaire grâce au default.

**Effort** : 20 min.

---

### H3. `UserProfileEntity.onboardingCompleted` forcé à `true` à l'import `[FIXED v3.7.0]`

**Fichier** : `BackupImporter.kt:80`

Pas de gros impact (un user qui restaure un backup a évidemment passé l'onboarding), mais c'est une perte de fidélité. Plus important : aucun champ n'est explicitement documenté comme "volontairement non backupé", et la liste s'allonge silencieusement à chaque release.

**Recommandation** : Soit ajouter le champ au backup, soit documenter dans `KalosBackup.kt` les champs explicitement omis avec leur raison.

**Effort** : 15 min.

---

### H4. `PRODUCT_AUDIT.md` est obsolète `[FIXED v3.7.0 — supprimé]`

**Fichier** : `docs/PRODUCT_AUDIT.md`

Le doc est figé à v3.2.0. Tous les "gaps identifiés" sont marqués Done. Le doc n'apporte plus de valeur informative à part la section "UX notes" et "Technical notes".

**Recommandation** : Soit supprimer ce fichier (`CHANGELOG.md` + `FEATURES.md` couvrent déjà tout), soit le réécrire comme audit produit (ergonomie, friction, parcours utilisateur) en complément du présent audit technique. Décision produit, pas technique.

**Effort** : 30 min (suppression) ou 2 h (réécriture).

---

### H5. `ActiveWorkoutStore.load()` catch silencieux

**Fichier** : `feature/workout/active/ActiveWorkoutStore.kt` (vu via agent, lignes ~56-59)

```kotlin
} catch (_: Exception) { null }
```

Toute exception de désérialisation du draft (par exemple suite à un nouveau champ requis sans default) renvoie `null`. Le ViewModel reprend une séance vierge → l'utilisateur perd sa session en cours sans alerte.

**Impact** : rare en pratique tant que les data classes ont des defaults, mais frustrant quand ça arrive.

**Recommandation** : Logger l'exception. Tagger les data classes du draft pour qu'elles tolèrent les champs absents (defaults), et ajouter `coerceInputValues = true` au `Json { }` du Store comme du Importer.

**Effort** : 30 min.

---

### H6. `@Insert(REPLACE)` sur tables avec enfants CASCADE

**Fichiers** : `WorkoutLogDao.kt:24, 40` ; `MealEntryDao.kt:17`

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertLog(log: WorkoutLogEntity): Long
```

En pratique l'appel est toujours fait avec `id = 0` donc le risque est théorique. **Mais** : si un jour quelqu'un passe un ID existant (par erreur ou par optimisation), l'INSERT REPLACE DELETE + INSERT, déclenchant la CASCADE → tous les sets/exercises du log sont supprimés silencieusement. Le bug est non-detectable côté code (pas de typage qui empêche).

**Recommandation** : Sur les parents avec CASCADE descendant, remplacer `REPLACE` par `ABORT` (échec bruyant) ou `IGNORE`. Garder `REPLACE` uniquement sur les feuilles sans enfants (`WorkoutLogSetEntity.upsertSet` est légitime — pas d'enfant à cascader).

**Effort** : 1 h pour auditer tous les `REPLACE` et choisir la stratégie.

---

## Findings moyennes (cleanup / robustesse)

### M1. `MealRepositoryImpl.getMealsForDate` : N+1 query sur les foods

**Fichier** : `core/data/repository/MealRepositoryImpl.kt:23-30` (lu via agent)

Chaque item de chaque entry fait un `foodDao.getById(item.foodId)` séquentiel. Sur une journée chargée (20 items), ça fait 20 requêtes synchrones avant de pouvoir afficher le journal.

**Recommandation** : Ajouter `FoodDao.getByIds(ids: List<Long>): List<FoodEntity>`, puis bâtir une `Map<Long, FoodEntity>` une seule fois par émission.

**Effort** : 45 min.

---

### M2. `WorkoutLogDetailViewModel` : N+1 sur `maxWeight`

**Fichier** : `feature/workout/history/WorkoutLogDetailScreen.kt:68-70`

Boucle `forEach { le -> getMaxWeight(le.exercise.id) }` → 1 + N requêtes par chargement.

**Recommandation** : Voir H1, à fusionner avec la déduplication des deux écrans.

**Effort** : inclus dans H1.

---

### M3. `BackupImporter` : pas de `coerceInputValues` `[FIXED v3.7.0]`

**Fichier** : `core/export/BackupImporter.kt:27`

```kotlin
private val json = Json { ignoreUnknownKeys = true }
```

`ignoreUnknownKeys` masque les **champs ajoutés** dans le backup mais inconnus du code. Il ne couvre pas le cas inverse : un backup ancien ne contient pas un champ rendu requis dans le code → exception.

Tant que toutes les data classes `*Backup` ont des defaults pour les nouveaux champs (c'est le cas aujourd'hui, mais fragile), ça tient. `coerceInputValues = true` aide pour les `null` envoyés sur des non-null.

**Recommandation** : Ajouter `coerceInputValues = true`. Documenter dans `KalosBackup.kt` la règle "**tout nouveau champ DOIT avoir un default**".

**Effort** : 15 min + doc.

---

### M4. `FoodRepositoryImpl.archiveOrDelete` non transactionnel

**Fichier** : `core/data/repository/FoodRepositoryImpl.kt:24-30`

```kotlin
if (dao.countUsage(id) == 0) dao.delete(...) else dao.setArchived(id, true)
```

Entre `countUsage` et `delete`, une autre coroutine peut insérer un `meal_entry_item` référençant cet aliment → la FK RESTRICT throw au moment du delete → crash similaire à celui qu'on vient de fixer.

En pratique très improbable (un seul user, un seul thread UI), mais c'est un footgun.

**Recommandation** : Wrapper dans `database.withTransaction { }`. Ou simplifier : toujours archiver, jamais delete (les aliments custom sont rarement nombreux, et l'archive est définitive vu qu'on n'a pas d'écran "restaurer un aliment archivé").

**Effort** : 30 min.

---

### M5. `MealEntryDao.insertEntry` REPLACE peut écraser ses propres items

**Fichier** : `core/database/dao/MealEntryDao.kt:17`

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertEntry(entry: MealEntryEntity): Long
```

Le `MealEntryEntity` n'a pas de contrainte unique sur `(date, mealType)` (à vérifier sur le schema). Donc en théorie pas de conflit. Mais si un jour quelqu'un ajoute cette contrainte unique sans changer le DAO, l'insertion de "Déjeuner 2026-05-28" supprimerait l'entry précédente + ses items via CASCADE.

**Recommandation** : Aujourd'hui ce n'est pas un bug, mais à noter si la contrainte unique est introduite plus tard. Aligner avec H6.

**Effort** : 0 h (vigilance).

---

### M6. `WorkoutLogDetailScreen` et `WorkoutSummaryScreen` redéfinissent `toWeightInput()`

**Fichiers** : `WorkoutLogDetailScreen.kt` et `WorkoutSummaryScreen.kt`

Deux extensions privées identiques `Float.toWeightInput()`. Banal mais sera vite un pattern si on duplique des dialogs.

**Recommandation** : Extraire dans `core/ui/util/Formatters.kt` (ou existant) à la prochaine session de refactor.

**Effort** : 5 min.

---

### M7. `seedIfEmpty()` exécuté à chaque démarrage

**Fichier** : `KalosApp.kt:21`, `DatabaseSeeder.kt:34-38`

```kotlin
scope.launch { seeder.seedIfEmpty() }
```

À chaque cold start, l'app lit `seed_exercises.json` (~150 entrées) et lance la phase différentielle. Sur un device modeste, c'est ~50-100ms d'IO+décodage à chaque ouverture, même quand il n'y a rien à faire.

La phase 1 (backfill seedId) itère **systématiquement** sur tous les seeds même quand `seed_exercises_version` est déjà à jour. Vérifier que le `return` ligne 42 short-circuit bien — ✅ c'est le cas. Donc tant que la version cache est à jour, on évite le travail. OK.

**Recommandation** : Aucune action. Le mécanisme `seed_exercises_version` est suffisant. À surveiller si on ajoute un `seed_foods_version`.

**Effort** : 0.

---

### M8. `LaunchedEffect(addedSuccessfully)` sans reset

**Fichier** : `feature/nutrition/search/FoodSearchScreen.kt` (vu via agent)

```kotlin
LaunchedEffect(state.addedSuccessfully) {
    if (state.addedSuccessfully) navController.popBackStack()
}
```

Le flag n'est jamais remis à `false` côté VM. Si l'écran est réutilisé (cas exotique mais possible avec deep links), le pop se redéclenche.

**Recommandation** : Soit reset après pop dans le VM, soit utiliser un `SharedFlow<Unit>` à la place du booléen (one-shot event).

**Effort** : 20 min.

---

### M9. `NutritionHistoryViewModel` borne fixe à 60 jours

**Fichier** : `feature/nutrition/history/NutritionHistoryViewModel.kt:21-24`

Fenêtre glissante hardcodée à 60 jours. Un utilisateur qui ouvre l'app après 80 jours d'absence ne voit que les 60 derniers — pas de pagination ni de message "données antérieures non chargées".

**Impact** : limité, mais peu propre. À comparer avec `WorkoutLogDao.getAll()` qui retourne tout sans limite (`ORDER BY startedAt DESC` sans LIMIT) → asymétrie potentiellement gênante côté perf si quelqu'un cumule des années de logs.

**Recommandation** : pas urgent. Décider produit : fenêtre fixe vs pagination. Si fenêtre, la rendre configurable et homogène entre nutrition et sport.

**Effort** : selon la décision, 1-3 h.

---

## Findings basses (notes)

### B1. `BackupExporter.json = Json { prettyPrint = true }`

Pretty print double la taille du JSON. À 1000 entrées de meal items, c'est ~200 KB → 400 KB. Non bloquant. Garder pour la lisibilité tant que personne ne se plaint de la taille.

### B2. Pas de validation de dates en string

**Plusieurs fichiers** : les dates sont stockées comme `String` ISO8601, comparées comme String dans les requêtes Room et dans le code (`state.date < LocalDate.now().toString()`). Fragile si une date est jamais malformée — mais aujourd'hui tout passe par `LocalDate.now().toString()` ou un picker qui produit du format propre. À surveiller si on ajoute un input texte de date.

### B3. `CustomFoodViewModel.isValid` jamais utilisé

**Fichier** : `feature/nutrition/custom/CustomFoodViewModel.kt:83-87`

Propriété définie mais le screen n'utilise pas pour gater le bouton "Enregistrer". Le bouton est seulement gated sur `isSaving`. Conséquence : `parseFloat() ?: 0f` fallback silencieux à 0 pour kcal/macros si l'utilisateur laisse un champ vide.

**Recommandation** : Brancher `enabled = isValid && !state.isSaving` sur le bouton.

**Effort** : 5 min.

### B4. `consolidate()` dans `NutritionScreen` non trié

**Fichier** : `feature/nutrition/NutritionScreen.kt:272-276` (vu via agent)

`groupBy { it.food.id }.values` retourne l'ordre d'insertion du LinkedHashMap, qui peut varier selon le tri amont. Suppression d'un item consolidé supprime **toutes** les instances de cet aliment dans le repas. Comportement actuel intentionnel (l'app affiche les doublons consolidés), mais à vérifier produit.

### B5. Refute d'un faux positif (agent nutrition)

L'agent nutrition signalait la logique `vegetarian/vegan` (`CustomFoodViewModel.onIsVeganChange / onIsVegetarianChange`) comme buguée. **Verdict après relecture** : la logique est correcte. Uncheck vegetarian ⇒ uncheck vegan ; check vegan ⇒ check vegetarian. Les deux autres branches sont des no-ops volontaires. Pas de correction à faire.

### B6. PLAN.md / gradle-wrapper.jar non gitignorés

`git status` montre `?? PLAN.md` et `?? gradle/wrapper/gradle-wrapper.jar` comme untracked. `gradle-wrapper.jar` devrait être committé (c'est le wrapper, fait partie du build) ; `PLAN.md` est un fichier de session à supprimer ou ignorer.

---

## Cohérence code ↔ documentation

| Document | État | Note |
|---|---|---|
| `CHANGELOG.md` | À jour (v3.5.0). Pas encore d'entrée pour v3.6.0 (clipboard export) ni pour le fix custom food d'aujourd'hui. | À mettre à jour avant la prochaine release. |
| `FEATURES.md` | À jour côté version (3.5.0) ; manque la mention du clipboard export 3.6.0 et la mention "Settings 3.6.0". | À mettre à jour. |
| `ROADMAP.md` | À jour ; section Deferred contient bien la note densité calorique. | OK. |
| `PRODUCT_AUDIT.md` | Obsolète (cf. H4). | À supprimer ou réécrire. |

---

## Roadmap de correction recommandée

### À faire maintenant (sprint immédiat, ~1 jour de travail)

1. **C2 partie b** : wrapper `BackupImporter.import()` dans une transaction. *(risque le plus élevé en blast radius)*
2. **C4** : validation des orphelins avant la purge dans l'import.
3. **C1 partie 1** : retirer `fallbackToDestructiveMigration()`, activer `exportSchema = true`, commit du dossier `app/schemas/`.
4. **H2** + **H3** : ajouter `lastUsedAt` et `onboardingCompleted` au backup (30 min, gain UX immédiat).
5. **M3** : `coerceInputValues = true` sur le `Json` de l'importer.
6. Mettre à jour `CHANGELOG.md` et `FEATURES.md` pour v3.6.0 et v3.6.1 (custom food fix).

### À faire la semaine suivante (~1 jour)

7. **C3** : pattern `runCatching` sur les ViewModels critiques (au minimum Custom food, Export, Import, finish séance, edit set).
8. **C2 partie a et c** : transactionner `finish()` et `saveSetEdit`.
9. **H1** : dédupliquer `WorkoutSummaryViewModel` et `WorkoutLogDetailViewModel` (extraire dialog + repository.editSet).
10. **H6** : auditer tous les `@Insert(REPLACE)`, choisir `ABORT` pour les parents avec CASCADE.

### À surveiller (pas urgent, à programmer quand on touche la zone)

11. **C1 partie 2** : premier test de migration paramétré (v11→v13).
12. **M1**, **M2** : éliminer les N+1 (foods, maxWeights).
13. **M4** : transaction sur `archiveOrDelete`.
14. **H4** : décision produit sur `PRODUCT_AUDIT.md` (supprimer ou réécrire).

### Backlog froid (qualité, pas blocant)

15. **M6**, **B3**, **B6** : nettoyages divers.
16. **M9** : décision produit pagination historique.
17. **B1** : taille du JSON export (réévaluer si quelqu'un se plaint).

---

## Ce qui est solide

- L'architecture globale et les conventions.
- La logique métier (BMR/TDEE/macros — Mifflin-St Jeor correctement implémenté, vu via `CalculateBmrUseCase` / `CalculateMacroGoalsUseCase`).
- La séparation snapshot/dynamique sur les macros nutritionnelles : décision produit cohérente et bien implémentée.
- Le seed différentiel exercices (v3.2.0) — c'est un pattern propre.
- Le rendu Compose (theming, dark mode, ElevatedCard, etc.) : pas de surprises.

## Ce qui est fragile

- Persistance : transactions absentes, exportSchema désactivé, fallback destructif actif.
- Gestion d'erreurs UI : quasi inexistante au niveau VM.
- Couplage clipboard / N+1 : pas d'index spécifiques pour les requêtes d'historique.
- Duplication entre les deux écrans qui éditent des sets.

## Correction immédiate vs attente

| À faire immédiatement | Peut attendre |
|---|---|
| C1, C2, C3, C4 — robustesse persistance & erreurs | M1, M2, M6, M9 — optimisations & cleanup |
| H1 — déduplication écrans édition de set | H4 — décision PRODUCT_AUDIT.md |
| H2, H3, M3 — qualité du backup | B1, B2, B3 — notes diverses |
