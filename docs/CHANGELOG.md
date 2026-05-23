# Kalos — Changelog

---

## v2.1.1 — Cohérence thème & hub notifications
_23 mai 2026_

### Ajouts
- **Hub Notifications** : nouvel écran `Paramètres > Notifications` centralisant tous les rappels
  - Rappels intelligents déplacés de Paramètres → Notifications
  - Rappels par programme avec toggle individuel + lien vers la fiche programme
  - Heure d'envoi configurable pour les rappels programme
- **Aperçu nutritionnel "Après ajout"** : bande projetée dans la fiche aliment
  - Affiche les totaux journaliers projetés vs objectif avant confirmation
  - Code couleur : normal / avertissement (>90%) / dépassement (>100%)

### Corrections
- Token `secondary` (orange/marron `#FB923C`) remappé sur vert dans `Color.kt`
  → Plus aucune couleur marron résiduelle dans l'UI
- Slider "Heure d'envoi" : ticks inactifs désormais cohérents (plus orange)
- Chips filtres ExerciseCatalog et ActiveWorkout : états sélectionnés unifiés sur `primaryContainer`

### Performance
- `CalendarGrid` : cellules memoïsées avec `remember(month)`
- `NutritionViewModel` : suppression des appels `distinctUntilChanged()` deprecated sur StateFlow

---

## v2.1.0 — Rappels intelligents + fix graphe fréquence
_22 mai 2026_

### Ajouts
- **Rappels intelligents** (`IntelligentReminderScheduler` + `IntelligentReminderWorker`)
  - Rappel Nutrition : si aucun repas enregistré dans la journée
  - Rappel Activité : si inactif depuis X jours (2/3/5 configurables)
  - Rappel Hydratation : si <50% objectif eau atteint
  - Heure d'envoi configurable (6h–22h, défaut 20h)
  - Interrupteur maître + toggles par type
  - Canal de notifications dédié `kalos_smart_reminders`

### Corrections
- Graphe fréquence d'entraînement : label "Cette sem." tronqué verticalement → corrigé

---

## v2.0.0 — Robustesse du mode workout
_22 mai 2026_

### Améliorations
- Sauvegarde automatique brouillon en séance active (debounce 400ms)
- Dialog de reprise si séance interrompue (avec avertissement si >24h)
- Écran résumé post-séance (durée, volume total, séries par exercice)
- Calcul du volume total (∑ reps × poids) persisté dans WorkoutLog

---

## v1.9.2 — Consolidation des aliments en double
_22 mai 2026_

### Corrections
- Repas : les doublons du même aliment sont groupés et affichés de manière consolidée
- Import données : correction de la logique de restauration

---

## v1.9.x — Import / Export de données
- Export JSON complet (profil, repas, séances, exercices custom)
- Import / restauration avec dialog de confirmation

## v1.8.x — Tags diététiques et préférences alimentaires
- Tags sur aliments custom (porc, alcool, végétarien, vegan)
- Filtres diététiques dans Paramètres, appliqués aux suggestions et à la recherche

## v1.7.x — Programmes d'entraînement
- 3 programmes seed (PPL 6j, Upper/Lower 4j, Full Body 3j)
- Activation de programme, affichage dans Home et Calendrier

## v1.x — Fondations (v1.0 → v1.6)
- Onboarding complet avec TDEE / macros
- Journal nutrition avec 4 repas, recherche, aliments custom
- Catalogue exercices (~100) avec filtres muscle/type
- Builder de séances + séance active
- Calendrier mensuel avec indicateurs
- Hydratation
- Suivi objectifs nutritionnels
- Mode sombre, thème Material3 vert/orange, police Nunito
