# Changelog

---

## v3.21.0 — 3 July 2026

### Changed — historique nutritionnel paginé

Quatrième lot de la revue générale.

- L'historique nutritionnel était figé à 60 jours : au-delà, les jours plus anciens n'étaient plus consultables. Il charge désormais 30 jours puis un bouton « Charger plus » élargit la fenêtre par tranches de 30 jours, jusqu'au premier jour enregistré (bouton masqué ensuite). Nouvelle requête `getEarliestMealDate` pour savoir quand il n'y a plus rien à charger.
- L'historique des séances reste sur le chargement complet (le graphe de volume dépend de l'ensemble) ; son N+1 a déjà été corrigé en v3.19.0.

---

## v3.20.0 — 3 July 2026

### Added — thème clair et choix du thème

Troisième lot de la revue générale. L'app n'était qu'en thème sombre.

- **Thème clair** complet, même identité de marque (vert émeraude, accents orange), adapté aux surfaces claires.
- **Sélecteur de thème** dans Paramètres > Apparence : Système / Clair / Sombre. « Système » suit le réglage de l'appareil.
- Préférence persistée (`ThemePreferenceStore`) et appliquée en direct sans redémarrage.
- Contraste des icônes de barre système (statut / navigation) aligné sur le thème résolu.
- Toute l'UI reposant déjà sur les tokens Material 3, le basculement s'applique à l'ensemble des écrans.

---

## v3.19.0 — 3 July 2026

### Added — variantes d'exercices

Deuxième lot de la revue générale. Distinction des variantes d'attache d'un même exercice, chacune avec sa propre progression/PR.

- Nouvelles variantes câble : **Extension triceps (barre)** (poussée à la barre droite/V, complément de la corde) et **Tirage poitrine prise serrée (supination)**.
- Convention de nommage « Exercice (attache) » homogénéisée sur la famille triceps (ex. « Extension triceps corde » -> « Extension triceps (corde) »).
- Nouvelle phase du seeder différentiel (Phase 5) : les renommages d'exercices du seed se propagent désormais aux installations existantes (par `seedId`, `nameNormalized` inclus pour la recherche). Les exercices personnalisés ne sont jamais touchés. `SEED_EXERCISES_VERSION` 4 -> 5.

### Performance

- **Fin du N+1 sur le chargement des séances/templates** : `WorkoutRepositoryImpl` résout les exercices en une requête groupée (`exerciseDao.getByIds`) au lieu d'une par exercice (`buildLog`, `getTemplates`, `getTemplate`). L'historique muscu ne déclenche plus des dizaines de requêtes à l'ouverture. Aucun changement de schéma.

---

## v3.18.0 — 3 July 2026

### Fixed — notifications

Premier lot de la revue générale du 3 July 2026 (voir `docs/TECHNICAL_AUDIT.md`).

- **Le tap ouvre enfin l'app** : les notifications embarquaient aucun `PendingIntent`. Elles ouvrent désormais l'app, avec deep-link vers l'écran pertinent (rappel nutrition -> Nutrition, rappel séance -> Séances, hydratation -> Nutrition).
- **Heure de rappel respectée** : le rappel intelligent passait par un `PeriodicWorkRequest` qui dérivait (Doze/batching) et tombait souvent en matinée quelle que soit l'heure choisie. Remplacé par un `OneTimeWorkRequest` auto-replanifié chaque jour à l'heure configurée. Effet de bord positif : la vérification « aucun repas enregistré aujourd'hui » se déclenche le soir (pertinent) et non le matin (faux positif systématique).
- **Robustesse** : replanification au lancement de l'app en filet de sécurité si la chaîne a été interrompue.

### Removed — suggestions nutrition

- Retrait de la carte « Suggestions » du journal nutritionnel (peu utilisée). Suppression de `SuggestFoodsUseCase` et `FoodTagger`.
- Bonus performance : le journal nutritionnel ne charge plus toute la table d'aliments à chaque changement de date (elle n'était combinée que pour alimenter les suggestions).

---

## v3.17.0 — 3 July 2026

### Added — bilan hebdomadaire

Écran de synthèse qui transforme les données déjà loguées en feedback, sur 7 ou 30 jours. Accessible depuis le Profil (« Bilan hebdomadaire »). Aucun changement de base de données (agrégation des données existantes).

- **Nutrition** : moyenne calories/j et protéines/j vs objectif (calories au-dessus de la cible en ambre), nombre de jours dans la cible calorique (90–105 % de l'objectif) et de jours protéines atteintes (≥ objectif), mini-graphe des calories/jour avec ligne d'objectif.
- **Poids** : variation sur la période + rythme en kg/semaine, avec un verdict par rapport à la direction de l'objectif du profil (en ligne / stagnation / direction opposée / stable). État vide propre si moins de deux pesées sur la période.
- **Entraînement** : séances de la semaine vs cible du programme actif, total sur la période, barres des séances/semaine sur 4 semaines.
- **À retenir** : phrase de synthèse qualitative choisie parmi plusieurs variantes selon la situation (nutrition/entraînement/poids) — stable pour un bilan donné, variée d'une période à l'autre — accompagnée des chiffres clés mis en avant en pastilles (jours dans la cible, protéines/j, séances, variation de poids). Règles simples, pas d'IA.
- Sélecteur de période 7 jours / 30 jours.

---

## v3.16.0 — 3 July 2026

### Added — repas favoris (meal templates)

Enregistrer un repas récurrent (ex. salade de thon) et le réappliquer en un tap. Livré en deux phases : données + sauvegarde (Phase A), puis interface (Phase B).

- **Base de données** : nouvelles tables `meal_template` et `meal_template_item` (migration 15 → 16, schema v16). L'aliment référencé par un favori est protégé de la suppression définitive (FK RESTRICT + archivage), comme pour l'historique.
- **Enregistrer comme favori** : depuis un repas déjà rempli, menu (⋮) → « Enregistrer comme favori », puis nommer. Les aliments en double sont fusionnés (grammages additionnés).
- **Appliquer un favori** : menu (⋮) d'un repas → « Ajouter un repas favori » → choix dans la liste. Les aliments sont **ajoutés** au repas (jamais de remplacement) sur la date affichée.
- **Gérer les favoris** : écran dédié (menu ⋮ de l'écran Nutrition → « Gérer les repas favoris ») listant chaque favori (kcal + aliments + grammages) avec suppression. Supprimer un favori n'affecte pas les repas déjà journalisés.
- **Détail et édition d'un favori** : taper un favori ouvre l'éditeur — renommer, ajuster le grammage de chaque aliment, retirer un aliment, en ajouter un via le sélecteur d'aliments (recherche/scan réutilisés en mode « choisir »). Bouton + pour créer un favori de zéro. Éditer un favori ne modifie jamais les repas déjà journalisés.
- **Sauvegarde / restauration** : les favoris sont inclus dans l'export/import JSON (remappage des IDs d'aliments personnalisés, ordre de purge FK-safe).

---

## v3.15.0 — 5 June 2026

### Added — indice de satiété / volume eating

Clôture de la piste "densité / volume eating" (`docs/ROADMAP.md`). Aide à repérer les aliments rassasiants pour peu de calories. Aucun changement de base de données (calcul depuis `kcalPer100g` + protéines + fibres).

- **Indice de satiété "rassasiant par calorie"** : règle transparente combinant densité énergétique (dominante) et macros rassasiantes (protéines + fibres), plutôt qu'un score opaque.
  - **Peu rassasiant** (ambre) : forte densité > 350 kcal/100 g (oléagineux, fromage, huiles) — dense par calorie même si nutritif
  - **Rassasiant** (vert) : faible densité < 150, ou densité modérée mais riche en protéines+fibres (≥ 12 g/100 g) — poulet, lentilles, légumes
  - **Modéré** sinon
- **Recherche / favoris / récents** : préfixe coloré "Rassasiant" / "Peu rassasiant" sur chaque aliment (les modérés restent neutres, pas de surcharge).
- **Feuille de portion** : indicateur "Rassasiant · 52 kcal/100 g" avec pastille colorée + les kcal/100 g bruts affichés, pour que le label ne trompe jamais seul.
- **Tri "Volume eating"** : chip dans la recherche/filtre qui classe les résultats du moins au plus dense — idéal pour trouver l'option la plus légère d'une catégorie.
- Jamais de rouge : signal positif (trouver du volume), pas culpabilisant — cohérent avec la logique vert→ambre du dépassement d'objectif.

Volets écartés (faible valeur) : comparaison vs moyenne de catégorie, densité moyenne journalière. Suggestions intelligentes à faible densité : possibles plus tard si l'usage le demande.

---

## v3.14.0 — 5 June 2026

### Added — scanner de code-barres alimentaire

Ajout d'un aliment par scan du code-barres. Implémenté en 3 phases (voir `docs/SCANNER_AUDIT.md`), livrées ensemble.

- **Écran de scan** : caméra (CameraX) + décodage ML Kit embarqué (modèle bundlé, pas de dépendance Google Play Services). Formats retail : EAN-13/8, UPC-A/E. Permission caméra demandée à l'usage, avec écran de repli si refusée.
- **Point d'entrée** : icône scan dans la barre de la recherche d'aliments.
- **Résolution en cascade, dégradable** :
  1. **Cache local** — code-barres déjà connu → ouvre directement la feuille de portion (instantané, offline).
  2. **OpenFoodFacts** — inconnu localement → interrogation en ligne ; si trouvé, ouvre "Nouvel aliment" **pré-rempli** (nom + macros) pour validation avant enregistrement (les données OFF sont communautaires).
  3. **Repli** — produit introuvable ou hors-ligne → création manuelle avec le code-barres pré-rempli.
- Tout produit enregistré est **mis en cache** avec son code-barres → un re-scan est instantané et offline.
- **Offline-first préservé** : le réseau n'est utilisé qu'en résolution opportuniste, jamais sur le chemin critique. Échec réseau → repli manuel silencieux.

### Migration / permissions / dépendances
- DB v14 → v15 : `ALTER TABLE food ADD COLUMN barcode TEXT` (nullable, migration triviale)
- Permissions ajoutées : `CAMERA` (feature optionnelle — l'app s'installe sur device sans caméra), `INTERNET` (première permission réseau de l'app, pour OpenFoodFacts uniquement)
- Dépendances : CameraX 1.3.4 + ML Kit barcode 17.3.0 (~+3-4 Mo d'APK, modèle bundlé). Résolution OpenFoodFacts via `HttpURLConnection` + kotlinx.serialization, sans lib réseau supplémentaire.
- `FoodBackup.barcode` ajouté (default null → compat backups existants)

---

## v3.13.0 — 4 June 2026

### Fixed — remplacement d'exercice débloqué
- Un exercice déjà remplacé (ou ajouté hors programme) ne proposait plus les boutons "Remplacer" / "Passer" — après un misclick sur le mauvais exercice de remplacement, il devenait impossible de le re-remplacer. Pire, après un kill de l'app, l'`originalTemplateExercise` (non persisté dans le draft) était perdu, faisant disparaître aussi "Annuler le remplacement" : l'exercice était totalement gelé.
- Les actions "Remplacer" et "Passer" sont désormais **toujours disponibles** quel que soit le statut de l'exercice (PLANNED / REPLACED / ADDED). La ligne de contexte ("Remplace : X" + "Annuler", ou "Hors programme") reste affichée au-dessus. Correctif purement UI — la logique de remplacement du ViewModel enchaînait déjà correctement les remplacements successifs.

### Added — historique récent en séance
- Sous le repère de charge (PR · Dernière séance), une ligne compacte affiche le détail des séries de la dernière séance pour l'exercice en cours : "Dernière fois : 10×6,8 · 10×11,3 · 9×15,8 · 7×15,8"
- Une seule ligne, texte atténué, défilement horizontal si longue — aide-mémoire minimaliste pour savoir où l'on en était sans quitter la séance
- Exercices à charge uniquement (comme le repère PR), masqué si pas d'historique
- Technique : `WorkoutLogDao.getLastSessionSets` (mêmes critères de "séance la plus récente" que le top-weight, donc cohérent), `ExerciseReference.lastSessionSets`. Aucune migration.

### Audit
- `docs/SCANNER_AUDIT.md` : audit préparatoire du scan de code-barres (tension offline-first vs feature réseau, options caméra/décodage/résolution, plan en 3 phases). Aucun code — décisions produit en attente.

---

## v3.12.0 — 4 June 2026

Clôture de la revue UX (lot cosmétique restant) + trois corrections de logique produit.

### Added — réordonnancement des exercices dans l'éditeur de séance
- Flèches haut/bas compactes sur chaque carte exercice (`WorkoutBuilderViewModel.moveExercise`) — fini le supprimer/re-ajouter pour réordonner
- L'ordre est persisté à l'enregistrement via `orderIndex` (mécanisme existant, aucun changement de schéma)

### Fixed — durée de séance honnête (idle gap exclu)
- `WorkoutDraft.lastSavedAt` : timestamp du dernier auto-save (cadence ~400 ms pendant l'activité)
- À la reprise d'un draft, l'écart d'inactivité (`now - lastSavedAt`) est exclu de la durée s'il dépasse 5 min — une séance interrompue 19 h n'est plus enregistrée comme une séance de 19 h. Les pauses courtes (repos entre séries, process death bref) restent comptées
- L'obsolescence (bannière + dialog de reprise) est désormais mesurée depuis la dernière activité et non depuis le début de séance — un draft repris puis re-abandonné vieillit correctement
- Drafts antérieurs (`lastSavedAt = 0`) : comportement inchangé (compat)

### Changed — programmes seedés vides retirés
- `seed_programs.json` : PPL et Upper/Lower retirés (coquilles sans séances liées, jamais activables) — seul Full Body 3 jours reste pour les nouveaux installs
- Suppression locale : tout programme **inactif** (seedé ou custom) peut désormais être supprimé depuis sa carte (icône poubelle grise + confirmation) — permet de nettoyer les coquilles héritées sur les installs existants

### Changed — lot cosmétique final de la revue UX
- Hydratation : "Autre" passe en outlined comme les boutons +250/+500/+750 (l'action la moins fréquente n'est plus la plus saillante)
- Stats de séance (résumé + détail historique) : `FlowRow` — sur petit écran, une stat passe à la ligne entière au lieu de couper "615 / kg"
- Carte "Poids corporel" du Profil : chevron trailing (cohérence avec les autres cartes navigantes)
- Avatar Profil : initiale du prénom au lieu de l'icône générique (fallback icône si nom vide)
- Paramètres : suppression du titre de section "Notifications" qui dupliquait le titre de l'item
- Pencil d'édition de série : 11 dp/45 % → 14 dp/60 % (découvrabilité)
- Historique Sport : l'icône copy isolée devient un bouton libellé "Copier l'historique"
- Header Nutrition : l'action copier passe dans un menu ⋮ avec libellé explicite "Copier le résumé du jour" ; l'icône historique (navigation fréquente) reste visible

---

## v3.11.0 — 4 June 2026

Vague 2 de la revue UX — les sujets structurants : états d'obsolescence, dépassement d'objectif, clarification produit.

### Added — gestion de l'obsolescence de la séance en cours (C2 complet)
- Au-delà de 24 h (`ActiveWorkoutStore.EXPIRY_MS`), la bannière passe en ton neutre (`surfaceVariant`) avec le titre "Séance interrompue" au lieu du vert engageant "Séance en cours"
- Nouvelle action "Abandonner" sur la bannière obsolète, avec dialog de confirmation (les séries saisies sont perdues) — `WorkoutViewModel.discardDraft()` → le `draftFlow` réactif retire la bannière instantanément
- Plus de drafts fantômes qui traînent indéfiniment avec une invitation à reprendre

### Added — dépassement d'objectif visible (I1)
- Nouveau token `ColorOverTarget` (orange ambré) : signal de pilotage, jamais rouge punitif
- Barres macros : la valeur consommée passe en orange quand elle dépasse l'objectif (ex. "162" orange dans "162 / 160 g")
- Anneau calories : au-delà de l'objectif, l'anneau passe en orange uni et le centre affiche "+99 kcal" en orange au lieu de "99 restants" en vert

### Changed — programmes sans séances liées (I5 complet)
- Bouton "Activer" désactivé quand un programme n'a aucune séance liée
- Caption explicative sur la carte : pointe vers le flux existant (éditeur de séance → "Rattacher à un programme")

### Added — clarification poids de référence vs dernière pesée (I4)
- Caption sous le TDEE : "Le poids ci-dessus est la référence du calcul — il peut différer de votre dernière pesée"
- Quand l'écart entre poids de référence et dernière pesée atteint 1 kg : bouton "Utiliser ma dernière pesée (X kg)" qui met à jour le poids de référence en un tap
- Le resync respecte la règle existante d'EditProfile : les objectifs nutritionnels ne sont recalculés que s'ils ne sont pas personnalisés (`isCustom == false`) — les macros réglées à la main ne sont jamais écrasées

---

## v3.10.1 — 4 June 2026

Vague de quick wins issue de la revue UX/UI complète (9 écrans). Aucun changement fonctionnel — uniquement qualité perçue, lisibilité et cohérence.

### Fixed
- Écran Programmes : le FAB ne recouvre plus le bouton "Activer" du dernier programme (`contentPadding bottom` 88dp)
- Titre du détail historique : "Séance d'hier" / "Séance d'aujourd'hui" au lieu de "Séance du hier" (élision française correcte)
- Durée écoulée : "il y a 19 h" au lieu de "il y a 19h35" (lisible comme une heure d'horloge) — helper partagé `formatElapsedSince`, appliqué à la bannière séance en cours et au dialog de reprise ; au-delà de 24 h : "il y a X j"
- Profil : séparateur décimal cohérent — "Poids" et "Objectif" suivent désormais la locale (87,0 → "87", 82.5 → "82,5") comme le reste de l'app

### Changed
- Nombres groupés via formatter localisé (`NumberFormat`, locale FR) : "23 673 kg" (volume hebdo), "2 921 kcal/j" (TDEE)
- Chip programmes : "Aucune séance liée" / "1 séance liée" / "N séances liées" au lieu du "N séances" ambigu
- Onglets Sport : onglets inactifs en gris (`onSurfaceVariant`), seul l'onglet actif est en vert — état de sélection enfin lisible
- Icônes destructives (croix du journal nutrition, poubelles de l'éditeur de séance) : grises au lieu de rouges — le rouge est réservé aux confirmations
- Barres macros : objectif inline "162 / 160 g" à droite de la barre, au lieu de "/160g" sur une ligne séparée en dessous (s'applique aussi à l'écran de résultat d'onboarding)
- Champs de saisie séance active : placeholder "0" au lieu de "kg" (redondant avec l'en-tête de colonne)

---

## v3.10.0 — 3 June 2026

### Added — repère de charge en séance
- Pendant une séance active, chaque exercice affiche un repère historique sous le groupe musculaire : **PR (record absolu) + top set de la dernière séance**. Aide-mémoire pour ne pas avoir à fouiller l'historique en plein effort.
- La mention "Dernière séance" est masquée quand elle égale le PR (évite la redondance).
- Affiché uniquement pour les exercices à charge (`REPS_WEIGHT`, `DURATION_WEIGHT`). Les exercices durée pure (cardio, planches) n'ont pas de repère.
- Masqué pour un exercice sans historique (première fois).
- Le repère se met à jour automatiquement si un exercice est ajouté ou remplacé en cours de séance.

### Technique
- `WorkoutLogDao.getLastSessionTopWeight(exerciseId)` : top set complété de la séance terminée la plus récente contenant l'exercice (requête dédiée — ne réutilise pas `getExerciseProgression` dont le `LIMIT 20 ASC` ne garantit pas la séance la plus récente au-delà de 20 séances).
- PR réutilise `getMaxWeight` (sans limite, donc correct).
- `WorkoutRepository.getExerciseReference` + modèle `ExerciseReference(prKg, lastSessionTopKg)`.
- `ActiveWorkoutViewModel` : `exerciseReferences: Map<Long, ExerciseReference>` chargé paresseusement via un collecteur réactif sur l'ensemble des ids d'exercices.
- Les chiffres proviennent des séances terminées uniquement — la séance en cours (draft) n'est pas encore persistée, donc aucune contamination.

---

## v3.9.1 — 3 June 2026

Petite vague UX — deux ajustements pour fluidifier l'usage quotidien.

### Empêcher les doublons d'exercice
- Workout Builder → Catalogue : les exercices déjà présents dans la séance sont **masqués** de la liste. Le mécanisme passe les `exercise.id` exclus via `savedStateHandle["excluded_exercise_ids"]` entre les deux écrans. Le catalogue standalone (depuis la TopAppBar de l'écran Sport) reste sans filtrage.
- Séance active → bottom sheet d'ajout/remplacement : les exercices déjà présents sont rendus **gris pâle avec "Déjà ajouté"**, tap désactivé. Couvre aussi le cas SKIPPED (un exercice passé ne peut pas être réajouté en doublon).

### Bannière "Séance en cours" sur l'écran Sport
- `ActiveWorkoutStore.draftFlow` réactif (`_version: MutableStateFlow` notifié à chaque `save()` / `clear()`).
- `WorkoutViewModel.uiState` combine désormais templates + draft → `DraftBannerState` (templateId, templateName, exerciseCount, startedAt).
- `WorkoutScreen` affiche une carte légère `primaryContainer` au-dessus des onglets : `Séance en cours · [Nom] · N exercices · il y a X min`. Tap (carte ou bouton) → reprise dans `ActiveWorkout`, le dialog "Séance en cours" existant prend le relais.
- Aucun popup automatique : carte non intrusive, présente uniquement si un draft est détecté.

---

## v3.9.0 — 3 June 2026

### Added (Bloc 1 — séance C Full Body + variantes câble manquantes)
- 5 nouveaux exercices seed :
  - `pec-deck-rear-delt-fly` — Pec deck arrière (deltoïde postérieur)
  - `cable-fly-poulie-haute` — Câble fly poulie haute
  - `cable-fly-poulie-basse` — Câble fly poulie basse
  - `oiseau-cable-poulie-haute` — Oiseau câble poulie haute (rear delt cable variant)
  - `leg-press-unilaterale` — Leg press unilatérale
- Programme seed `Full Body — 3 jours` : la 3e séance bascule de "Full Body A" répété à "Full Body C" (impact nouveaux installs uniquement)
- `SEED_EXERCISES_VERSION` 3 → 4 → la phase 2 du seeder différentiel ajoute automatiquement les 5 nouveaux exercices chez tous les utilisateurs existants

### Added (Bloc 2 — exercices time-based)
- Nouveau champ `Exercise.trackingMode` (`ExerciseTrackingMode` enum) : valeurs `REPS_WEIGHT` (défaut, musculation), `DURATION` (cardio + holds isométriques), `DURATION_WEIGHT` (gainage lesté / farmer's walk)
- `SetRow` (séance active) rend désormais des champs conditionnels selon le `trackingMode` de l'exercice :
  - `REPS_WEIGHT` → poids + reps (comportement actuel)
  - `DURATION` → champ unique `mm:ss`
  - `DURATION_WEIGHT` → poids + `mm:ss`
- `EditWorkoutSetDialog` (résumé + historique) adopte la même logique conditionnelle
- Le résumé de séance et le détail historique formattent l'affichage des séries en fonction du `trackingMode` : un set de marche rapide affiche maintenant `25:00` au lieu de `0 × 0.0 kg`
- Pré-existant : `WorkoutLogSetEntity.durationSecs` était déjà dans le schéma — la valeur n'était jamais consommée, c'est désormais le cas

### Migration DB
- v13 → v14 : `ALTER TABLE exercise ADD COLUMN trackingMode TEXT NOT NULL DEFAULT 'REPS_WEIGHT'`
- Phase 4 nouvelle dans `DatabaseSeeder.seedExercisesDifferential()` : aligne le `trackingMode` des rows seed sur les valeurs du JSON courant. Tourne à chaque bump de version. Exercices custom (`seedId IS NULL`) jamais touchés.

### Seed updates (trackingMode populé)
- `DURATION` : `course-a-pied`, `velo-elliptique`, `rameur`, `velo-stationnaire`, `corde-a-sauter`, `ski-erg`, `natation-crawl`, `jumping-jacks`, `marche-rapide`, `mountain-climber`, `sprint-100m`, `assault-bike`, `hiit-tabata`, `planche`, `planche-laterale`, `copenhagen-plank`
- `DURATION_WEIGHT` : `farmer-s-walk`

### Notes
- Le volume total continue d'être calculé sur `reps × weight` — les exercices cardio contribuent 0, comportement attendu (pas de régression)
- Les exercices `DURATION` n'affichent pas de PR pondéral (max weight) — la progression chart par exercice reste basée sur le poids
- Pour les utilisateurs existants : le programme Full Body 3-jours **ne sera pas modifié automatiquement** côté templates. Le template "Full Body C" doit être créé manuellement via Workout Builder
- Backup compatible : `LogSetBackup.durationSecs` existait déjà → aucun changement de format JSON

---

## v3.8.1 — 28 May 2026

Petite vague de nettoyage post-audit — 3 fixes ciblés sur les items moyens restants.

### Performance
- `MealRepositoryImpl.getMealsForDate` : fin du N+1 sur les `food`. Les items étaient résolus 1 par 1 via `foodDao.getById` (15 items = 15 requêtes synchrones par émission du Flow). Désormais un seul `foodDao.getByIds(distinct)` + `Map<Long, FoodEntity>` lookup. Les items sont également groupés par `mealEntryId` une seule fois au lieu de filtrer N fois
- `FoodDao.getByIds(ids)` : nouvelle requête `WHERE id IN (...)`

### Robustesse
- `FoodRepositoryImpl.archiveOrDelete` : encapsulé dans `database.withTransaction { }`. Sans transaction, une `addItemToMeal` concurrente entre `countUsage` et `delete` aurait pu déclencher une violation FK RESTRICT
- `FoodSearchViewModel.addToMeal` : pattern `runCatching` + champ `errorMessage` dans le state, surfacé via snackbar dans `FoodSearchScreen`. Plus de faux succès silencieux si l'écriture échoue

### Fixed
- `FoodSearchViewModel.addedSuccessfully` : flag réinitialisé via `onAddHandled()` après le `popBackStack`. Évite un re-déclenchement de la navigation si l'écran est réutilisé (deep link, back twice, etc.)

---

## v3.8.0 — 28 May 2026

### Changed (atomicité opérations workout)
- `WorkoutRepository.completeWorkout(log, durationSecs)` : nouvelle API transactionnelle pour terminer une séance — insère le log, ses exercices, ses sets, et met à jour `durationSecs` + `totalVolumeKg` dans une seule transaction Room. Rollback automatique en cas d'échec, plus de séances partielles dans l'historique
- `WorkoutRepository.editSet(logId, exerciseId, set)` : upsert d'une série + recalcul du volume total dans la même transaction. Remplace la séquence non-atomique `upsertSet + getLog + finishLog`
- `WorkoutLogDao.getMaxWeights(exerciseIds)` : nouvelle requête batch qui retourne `Map<exerciseId, maxWeight>` en une passe — élimine le N+1 sur `WorkoutLogDetailScreen` (un log avec 15 exercices générait 1+15 requêtes, désormais 1)

### Refactored (déduplication)
- `core/ui/component/EditWorkoutSetDialog.kt` : composable partagé extrait depuis `WorkoutSummaryScreen` et `WorkoutLogDetailScreen` — fin du dialog dupliqué, fin du `Float.toWeightInput()` dupliqué
- Bugfix dans l'un = bugfix partout

### Hardened (Insert REPLACE → ABORT sur parents avec CASCADE)
- `WorkoutLogDao.insertLog` et `insertLogExercise` : `OnConflictStrategy.REPLACE` → `ABORT`. Un appel avec un id non-nul (programming error) lèvera désormais une exception au lieu de CASCADE-supprimer silencieusement tous les enfants
- `MealEntryDao.insertEntry` et `insertItem` : idem
- `WorkoutLogDao.upsertSet` reste en REPLACE (intentionnel pour l'édition de série, pas d'enfants à cascader)

### Fixed (`ActiveWorkoutStore`)
- `load()` : l'exception de désérialisation est désormais loggée (`Log.w("ActiveWorkoutStore", ...)`) au lieu d'être silencieusement avalée — un draft corrompu sera visible dans les bug reports plutôt que perdu sans trace
- `Json { coerceInputValues = true }` ajouté pour tolérer les `null` envoyés sur des champs non-null avec default

---

## v3.7.0 — 28 May 2026

### Changed (robustesse persistance)
- Backup import désormais **atomique** : la séquence "purge + ré-insertion" est encapsulée dans `database.withTransaction { }` — toute exception en cours d'import rollback complètement, les données précédentes sont préservées
- Validation préalable des FK : `BackupImporter.validateReferences` vérifie tous les `foodId` (meal items) et `templateId` (program workouts) **avant** la purge ; en cas d'orphelin, l'import échoue sans toucher à la DB
- `BackupImporter.json` : ajout de `coerceInputValues = true` (tolère les `null` sur des champs non-null avec defaults)
- Migrations Room : retrait de `fallbackToDestructiveMigration()` au profit de `fallbackToDestructiveMigrationOnDowngrade()` — une migration manquante crashera désormais bruyamment au démarrage au lieu de wiper silencieusement les données utilisateur
- `@Database(exportSchema = true)` + KSP `room.schemaLocation = $projectDir/schemas` : les schémas Room sont désormais exportés sous `app/schemas/` (diffables en PR, requis pour MigrationTestHelper)

### Added (backup fidélité)
- `FoodBackup.lastUsedAt` : préserve l'ordre "Récents" après une restauration
- `ProfileBackup.onboardingCompleted` : préserve l'état réel (n'est plus forcé à `true`)
- Les deux champs ont des defaults pour préserver la compat des backups existants

### Fixed (gestion d'erreurs UI)
- `CustomFoodViewModel` : `save()` / `delete()` ne perdent plus silencieusement les erreurs DB — snackbar d'erreur dans `CustomFoodScreen`, l'utilisateur reste sur le formulaire
- `ActiveWorkoutViewModel.finish()` : le draft n'est plus effacé avant la confirmation du write ; si l'enregistrement échoue, le draft est conservé pour permettre le retry, snackbar d'erreur
- `WorkoutSummaryViewModel.saveSetEdit()` et `WorkoutLogDetailViewModel.saveSetEdit()` : pattern `runCatching` + snackbar d'erreur identique
- `errorMessage: String?` ajouté aux `UiState` des 4 ViewModels concernés, `onErrorShown()` pour le reset

### Removed
- `docs/PRODUCT_AUDIT.md` : doc obsolète (figé à v3.2.0, tous les items marqués Done) — remplacé par `docs/TECHNICAL_AUDIT.md` ; un nouvel audit produit sera créé le jour où il y a un besoin réel

---

## v3.6.1 — 27 May 2026

### Fixed
- Crash à l'édition d'un aliment custom déjà utilisé dans la journée : `FoodRepositoryImpl.save` utilisait `@Insert(REPLACE)` qui DELETE+INSERT, violant la FK RESTRICT depuis `meal_entry_item` → désormais `@Update` quand l'id existe, `REPLACE` réservé aux créations
- Édition d'un aliment custom : `isFavorite` et `lastUsedAt` étaient réinitialisés silencieusement à chaque save — désormais préservés via le state du `CustomFoodViewModel`

---

## v3.6.0 — 27 May 2026

### Added
- Historique nutritionnel : bouton de copie dans la TopAppBar — exporte les 14 derniers jours en TSV (Date, Kcal, Protéines, Glucides, Lipides) avec lignes Moyenne et Total, prêt à coller dans un tableur
- Historique sport (vue standalone) : bouton de copie dans la TopAppBar
- Historique sport (onglet) : icône de copie inline au-dessus du chart
- Format sport : résumé lisible humain des 10 dernières séances — date, nom, durée, volume total, puis meilleure série complétée par exercice

### Roadmap
- Note produit ajoutée dans les Deferred : densité calorique & aide au volume eating (signal de densité, tri, indice de satiété, comparaison volume / calories)

---

## v3.5.0 — 27 May 2026

### Added
- Édition d'une série depuis le résumé de séance (`WorkoutSummaryScreen`) : taper sur une série complétée ouvre une dialog de modification reps / poids ; sauvegarde via `upsertSet` + recalcul du volume total (`finishLog`)
- Édition d'une série depuis le détail d'une séance passée (`WorkoutLogDetailScreen`) : même comportement, idem recalcul volume
- Icône crayon discrète (11 dp, 45 % d'opacité) en trailing sur chaque ligne de série cliquable

### Fixed
- Saisie parasites dans les champs reps / poids de `SetRow` : focus sur un champ sélectionne désormais tout le texte (pattern `TextFieldValue` + `onFocusChanged`), ce qui évite le cas « 10 » → taper « 8 » → « 108 »
- Seed exercises : suppression de 2 exercices en double, ajout de 12 variantes manquantes

---

## v3.4.0 — 26 May 2026

### Fixed
- Export : `appVersion` prenait la valeur codée en dur `"1.7.0"` — remplacé par `PackageManager.getPackageInfo().versionName`
- Export : `FoodBackup` ne sauvegardait pas `sugarPer100g` → champ absent à l'import
- Export : `TrainingProgramBackup` ne sauvegardait pas `isCustom` → les programmes personnalisés perdaient leur statut à la restauration

---

## v3.3.0 — 26 May 2026

### Added
- Recherche d'exercices insensible aux accents : colonne `nameNormalized` sur `ExerciseEntity`, remplie via `normalizeForSearch()` (NFKD + strip diacritiques + lowercase + collapse espaces)
- Seeder v3 : backfill `nameNormalized` pour les installations existantes à la mise à jour
- DB version 13, `MIGRATION_12_13` : `ALTER TABLE exercise ADD COLUMN nameNormalized TEXT NOT NULL DEFAULT ''`
- Requêtes de filtre (`ExerciseDao`) portées sur `nameNormalized` ; query normalisée dans `ExerciseCatalogViewModel` et `ExercisePickerViewModel`

### Changed
- Suppression du debounce 300 ms sur la recherche d'exercices : la recherche SQLite locale (< 5 ms) n'en a pas besoin — la frappe est désormais immédiatement reflétée dans les résultats

---

## v3.2.0 — 25 May 2026

### Added
- Migration de seed différentielle pour les exercices : les utilisateurs existants reçoivent désormais les nouveaux exercices ajoutés en base à chaque mise à jour, sans écraser les exercices personnalisés
- Champ `seedId` (slug stable) sur chaque exercice seed dans `seed_exercises.json` et `ExerciseEntity`, avec index UNIQUE partiel (`WHERE seedId IS NOT NULL`) pour ne pas contraindre les exercices custom (`seedId = NULL`)
- `DatabaseSeeder.seedExercisesDifferential()` : phase 1 backfill (attribution du `seedId` aux lignes existantes par correspondance de nom), phase 2 insertion des nouveaux exercices absents de la DB ; version trackée dans `SharedPreferences "kalos_seed"` (`seed_exercises_version = 2`)
- DB version 12, `MIGRATION_11_12` : `ALTER TABLE exercise ADD COLUMN seedId TEXT DEFAULT NULL` + index UNIQUE partiel
- 143 exercices seed avec IDs stables (slugs normalisés, sans accents, sans espaces)

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
