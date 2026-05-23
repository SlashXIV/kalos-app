# Kalos — Roadmap

> Dernière mise à jour : v2.1.1 — 23 mai 2026

---

## Prochaines évolutions recommandées

Classées par impact utilisateur / effort d'implémentation.

---

### 1. Suivi du poids corporel
**Impact : Élevé · Effort : Faible**

L'entité `BodyWeightEntity` (date + weightKg) existe déjà en base. Il suffit de créer :
- Un écran ou une section dans Profil pour saisir le poids du jour
- Un graphe de courbe de poids (Vico, déjà intégré) sur les 30/90 derniers jours
- Un accès depuis le dashboard (Home) ou le calendrier

C'est la fonctionnalité la plus attendue d'un tracker fitness et la moins chère à faire.

---

### 2. Historique sport complet
**Impact : Élevé · Effort : Moyen**

Actuellement stub. À construire :
- Liste des séances passées avec détail (exercices, séries, volume)
- Graphe de volume total par semaine (barres, Vico)
- Records personnels par exercice (max poids, max volume) — `GetPersonalRecordsUseCase` existe déjà
- Tap sur une séance → détail complet

C'est le feedback loop essentiel pour la motivation en musculation.

---

### 3. Détail journée dans l'historique nutrition
**Impact : Moyen · Effort : Faible**

L'écran `NutritionHistoryScreen` liste 60 jours de résumés mais un tap ne fait rien. Il suffit de :
- Naviguer vers `NutritionScreen` avec la date sélectionnée (en mode lecture ou navigation)
- OU ouvrir un bottom sheet avec le détail des repas du jour

Peu de code, fort gain de cohérence UX.

---

### 4. Graphe de progression par exercice
**Impact : Moyen · Effort : Moyen**

Dans la fiche exercice (`ExerciseDetailScreen`), ajouter un graphe Vico montrant l'évolution du 1RM estimé ou du max poids utilisé sur les 30 dernières séances. Très motivant pour l'utilisateur qui suit sa progression.

---

### 5. Widget Android (écran d'accueil)
**Impact : Moyen · Effort : Élevé**

Un widget Glance (Jetpack Glance) affichant :
- Calories restantes du jour
- Anneau de progression simplifié
- Bouton "Logger un repas"

Nécessite Jetpack Glance + un WorkManager pour rafraîchir. Très visible pour l'engagement quotidien.

---

## Idées futures (non priorisées)

- Scanner code-barres pour ajouter des aliments (OpenFoodFacts API)
- Mode multi-utilisateurs / profils
- Export CSV / Google Sheets
- Partage de séances / programmes
- Objectifs hebdomadaires personnalisés
- Coaching AI basé sur les données de progression

---

## Ce qui est terminé (changelog rapide)

| Version | Points clés |
|---|---|
| v1.x | Fondations : nutrition, exercices, onboarding, thème |
| v2.0.0 | Robustesse mode workout (draft, reprise, résumé) |
| v2.1.0 | Rappels intelligents (WorkManager), fix graphe calendrier |
| v2.1.1 | Hub notifications, aperçu nutritionnel "Après ajout", cohérence thème (token secondary → vert) |
