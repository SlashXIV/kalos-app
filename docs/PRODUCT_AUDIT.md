# Kalos — Audit produit

> Version auditée : **2.1.1** — 23 mai 2026

---

## Vue d'ensemble

Kalos est un tracker fitness & nutrition offline-first, entièrement en français, ciblant Android 8.0+. L'application couvre les trois piliers d'un suivi de forme : alimentation, entraînement, et progression dans le temps.

**Maturité globale : production-ready** sur les fonctions principales. Deux modules secondaires restent partiels (historique sport, programmes custom).

---

## Modules

### Onboarding
**État : complet**

Flow en 4 étapes : Bienvenue → Profil (âge, sexe, taille, poids, objectif de poids) → Objectif (activité, but) → Résultat (BMR / TDEE / macros calculés). La formule Mifflin-St Jeor est correctement implémentée et documentée visuellement sur l'écran résultat.

Pas de possibilité de relancer l'onboarding depuis les paramètres (non-bloquant : tout est modifiable depuis "Modifier le profil" et "Modifier les objectifs").

---

### Home (Dashboard)
**État : complet**

Tableau de bord contextuel et vivant. L'anneau calorique et les barres macros reflètent les données du jour en temps réel. La carte programme est particulièrement bien faite : elle distingue "jour de séance" vs "jour de repos" et affiche la prochaine séance planifiée.

Points de polish déjà réalisés : données dynamiques, raccourcis rapides, liste des séances du jour.

---

### Nutrition
**État : complet — module le plus abouti**

Le journal quotidien est le cœur de l'app et il est bien fini :
- Navigation par date fluide
- 4 sections repas correctement séparées
- Hydratation avec ajouts rapides et corrections négatives
- Suggestions d'aliments intelligentes (basées sur macros restantes + filtres diét.)
- Fiche aliment avec projection "Après ajout" (totaux projetés vs objectif, code couleur)
- Aliments personnalisés complets avec tags diététiques

**Limite principale** : L'historique nutrition affiche des résumés (60j) mais ne permet pas de voir le détail d'une journée (quels repas, quels aliments). Un tap sur une ligne d'historique ne fait rien.

---

### Sport
**État : complet sur le chemin critique, partiel sur les modules secondaires**

Le flux principal — créer une séance → l'exécuter → la terminer — est solide et bien pensé :
- Builder avec liaison programme
- Tracker actif avec chrono, séries, minuterie de repos, sauvegarde brouillon, et reprise
- Résumé post-séance

**Manques identifiés :**

1. **Historique sport** : actuellement un stub. On voit la liste des séances passées mais sans interaction, sans détail, sans courbes de progression. C'est le vide le plus visible pour un utilisateur qui pratique régulièrement.

2. **Programmes custom** : les 3 programmes seed sont bien affichés, et on peut les activer / consulter leur détail. La création et l'édition de programmes personnalisés existent partiellement dans l'UI.

3. **Suivi du poids corporel** : l'entité `BodyWeightEntity` existe dans la base de données mais il n'y a aucun écran pour saisir ou visualiser l'évolution du poids. Gap fort étant donné que c'est affiché dans le profil.

---

### Calendrier
**État : complet**

Grille mensuelle propre avec indicateurs colorés (nutrition / séance faite / planifiée). La carte de détail journalier est bien réalisée et les deux insights (fréquence d'entraînement 4 semaines, moyenne calorique 7j) donnent une vraie lecture de tendance.

La correction de la troncature du label "Cette sem." est bien en place (v2.1.0).

---

### Profil
**État : complet**

Affichage clair des données physiques et objectifs. L'édition du profil recalcule automatiquement les macros si l'objectif ou le niveau d'activité change. L'écran "Modifier les objectifs" est particulièrement bien fait avec son breakdown détaillé avant enregistrement.

**Gap** : Aucune UI pour le suivi du poids corporel (voir section Sport ci-dessus).

---

### Paramètres & Notifications
**État : complet**

Les préférences diététiques sont bien intégrées dans toute la chaîne (filtrage des suggestions, filtrage des résultats de recherche). Export / import JSON fonctionnel avec confirmation.

Les notifications sont centralisées dans un écran dédié (depuis v2.1.0) : rappels intelligents (WorkManager daily) + rappels programme par toggle. Bien architecturé.

---

## UX globale

**Points forts :**
- Cohérence visuelle : palette verte / orange appliquée correctement depuis v2.1.1 (plus de token marron)
- Transitions fluides (fade + slide, 200ms)
- Formulaires intelligents (séparateur décimal français, validation live, désactivation des boutons si invalide)
- Feedbacks utilisateur présents (snackbars, spinners, dialogs de confirmation)
- Architecture réactive : l'UI se met à jour en temps réel (StateFlow)

**Points à surveiller :**
- L'historique sport vide crée une attente non satisfaite
- Pas de photo de profil (mineur, non attendu)
- La navigation "Profil" n'est pas dans la bottom bar directement (mais accessible via l'onglet 4)

---

## Qualité du code

| Critère | Évaluation |
|---|---|
| Architecture | MVVM + Repository + UseCase bien séparé |
| Réactivité | StateFlow + combine() correctement utilisés |
| DI | Hilt proprement configuré |
| Persistance | Room + fallbackToDestructiveMigration (acceptable en dev) |
| Notifications | WorkManager, EntryPoint pattern (non @HiltWorker) |
| Performance | Memoization Compose (remember(key)) en place sur CalendarGrid |
| Thème | Material3 dark, tokens bien mappés depuis v2.1.1 |

---

## Gaps et zones à traiter (priorisés)

| Priorité | Zone | Description |
|---|---|---|
| 🔴 Haute | Suivi poids corporel | Entité DB existante, aucune UI. Manque fort pour un tracker fitness |
| 🔴 Haute | Historique sport | Stub actuel : liste sans interaction ni graphes de progression |
| 🟡 Moyenne | Détail historique nutrition | Tap sur jour → afficher les repas de ce jour |
| 🟡 Moyenne | Programmes custom | Création / édition partielle |
| 🟢 Basse | Favoris exercices | Catalogue sans favoris |
| 🟢 Basse | Filtres avancés aliments | Recherche sans filtre catégorie |
