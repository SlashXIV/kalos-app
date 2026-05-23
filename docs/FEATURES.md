# Kalos — Inventaire des fonctionnalités

> Dernière mise à jour : v2.1.1

Légende : ✅ Terminé · ⚠️ Partiel · 🔲 Non fait

---

## Onboarding

| Fonctionnalité | État | Notes |
|---|---|---|
| Écran d'accueil (Welcome) | ✅ | Branding + liste de features |
| Configuration du profil (âge, sexe, taille, poids, objectif poids) | ✅ | |
| Sélection niveau d'activité + objectif | ✅ | 5 niveaux, 5 objectifs |
| Calcul BMR / TDEE / macros (Mifflin-St Jeor) | ✅ | Affiché sur écran résultat |
| Persistance profil + objectifs au premier lancement | ✅ | |
| Seeding base de données (aliments, exercices, programmes) | ✅ | Une seule fois, au premier lancement |

---

## Accueil (Home)

| Fonctionnalité | État | Notes |
|---|---|---|
| Anneau de progression calorique du jour | ✅ | Consommé / Objectif / Restant |
| Barres macros du jour (P / G / L) | ✅ | |
| Raccourcis rapides (Journal nutrition, Séance sport) | ✅ | |
| Carte programme du jour (si séance planifiée) | ✅ | Bouton "Démarrer la séance" |
| Carte jour de repos (si programme actif, pas de séance) | ✅ | Affiche la prochaine séance planifiée |
| Liste des séances terminées aujourd'hui | ✅ | Nom + nb exercices + durée |

---

## Nutrition

### Journal quotidien

| Fonctionnalité | État | Notes |
|---|---|---|
| Navigation par date (← →, retour à aujourd'hui) | ✅ | Futur désactivé |
| Résumé journalier (anneau calories + macros) | ✅ | |
| 4 sections repas (Petit-déjeuner / Déjeuner / Dîner / Collations) | ✅ | |
| Ajout aliment par repas → recherche | ✅ | |
| Suppression d'un aliment d'un repas | ✅ | |
| Consolidation des doublons (affichage groupé) | ✅ | |
| Copie du résumé journalier (presse-papier) | ✅ | |
| Suivi hydratation (compteur eau en ml) | ✅ | Ajout rapide 250/500/750ml + personnalisé |
| Objectif hydratation configurable | ✅ | Dialog in-screen |
| Valeurs négatives pour corrections hydratation | ✅ | |
| Suggestions intelligentes d'aliments | ✅ | Basé sur macros restantes + filtres diététiques |

### Recherche d'aliments

| Fonctionnalité | État | Notes |
|---|---|---|
| Recherche texte avec debounce (300ms) | ✅ | |
| Aliments récents (si aucune requête) | ✅ | |
| Aliments favoris (si aucune requête) | ✅ | |
| Fiche aliment (bottom sheet) | ✅ | Modes portion / grammes |
| Projection "Après ajout" (totaux journaliers projetés vs objectif) | ✅ | Code couleur normal / avertissement / dépassement |
| Filtres diététiques appliqués aux résultats | ✅ | |
| Création d'un aliment personnalisé depuis la recherche | ✅ | |

### Aliments personnalisés

| Fonctionnalité | État | Notes |
|---|---|---|
| Création d'aliment avec macros per 100g | ✅ | |
| Édition d'un aliment existant | ✅ | |
| Tags diététiques (porc, alcool, végétarien, vegan) | ✅ | Logique d'interdépendance (vegan → végétarien) |
| Séparateur décimal français (, accepté) | ✅ | |

### Historique nutrition

| Fonctionnalité | État | Notes |
|---|---|---|
| Résumés journaliers sur 60 jours | ✅ | Kcal + P/G/L par jour |
| Détail d'une journée (quels aliments) | 🔲 | Tap sur un jour → rien pour l'instant |

---

## Sport

### Catalogue d'exercices

| Fonctionnalité | État | Notes |
|---|---|---|
| Liste de ~100 exercices | ✅ | Données seed |
| Recherche texte | ✅ | |
| Filtres par groupe musculaire (13 options) | ✅ | |
| Filtres par type (5 options) | ✅ | |
| Fiche exercice (muscles, équipement, description) | ✅ | |
| Mode "Ajouter à la séance" (depuis le builder) | ✅ | Comportement dual selon contexte |

### Builder de séances

| Fonctionnalité | État | Notes |
|---|---|---|
| Création d'un template de séance | ✅ | |
| Édition d'un template existant | ✅ | |
| Ajout / suppression d'exercices | ✅ | |
| Édition sets × reps par exercice | ✅ | |
| Liaison programme + jour de semaine | ✅ | |
| Ordre des exercices | ✅ | Indexé, affiché dans l'ordre |

### Séance active (workout tracker)

| Fonctionnalité | État | Notes |
|---|---|---|
| Chronomètre séance (temps réel) | ✅ | Survit au verrouillage écran |
| Navigation par onglets entre exercices | ✅ | |
| Saisie poids / reps par série | ✅ | |
| Marquage série complétée | ✅ | |
| Ajout / suppression de séries | ✅ | |
| Minuterie de repos automatique (après série complétée) | ✅ | Durée configurable par exercice |
| Skip repos | ✅ | |
| Sauvegarde automatique brouillon (debounce 400ms) | ✅ | |
| Reprise de séance interrompue | ✅ | Dialog avec temps écoulé + avertissement si >24h |
| Fin de séance + sauvegarde dans l'historique | ✅ | |

### Résumé de séance

| Fonctionnalité | État | Notes |
|---|---|---|
| Résumé post-séance (durée, volume total, exercices) | ✅ | |
| Détail des séries complétées par exercice | ✅ | |

### Historique sport

| Fonctionnalité | État | Notes |
|---|---|---|
| Liste des séances passées | ⚠️ | Stub minimal, liste sans interaction |
| Détail d'une séance historique | 🔲 | |
| Graphe de volume / progression | 🔲 | |

### Programmes d'entraînement

| Fonctionnalité | État | Notes |
|---|---|---|
| Affichage des 3 programmes seed (PPL, U/L, Full Body) | ✅ | |
| Activation / désactivation d'un programme | ✅ | |
| Détail programme (jours + templates associés) | ✅ | |
| Création / édition de programmes custom | ⚠️ | UI partielle |
| Rappels jour J / veille par programme | ✅ | Dans ProgramDetailScreen |

---

## Calendrier

| Fonctionnalité | État | Notes |
|---|---|---|
| Grille mensuelle avec navigation mois | ✅ | |
| Indicateurs par jour (nutrition, séance faite, planifiée) | ✅ | Points colorés |
| Légende des indicateurs | ✅ | |
| Carte détail d'une journée (tap sur un jour) | ✅ | Nutrition + hydratation + séances |
| Lien "Voir nutrition" depuis le détail | ✅ | |
| Insight fréquence d'entraînement (4 semaines) | ✅ | Graphe à barres |
| Insight moyenne calorique (7 jours) | ✅ | Avec code couleur vs objectif |

---

## Profil

| Fonctionnalité | État | Notes |
|---|---|---|
| Affichage profil (poids, taille, objectif, TDEE) | ✅ | |
| Affichage objectifs nutritionnels | ✅ | |
| Édition profil complet | ✅ | Recalcul auto des macros si objectif/activité changent |
| Édition objectifs (manuel ou auto-calculé) | ✅ | Breakdown détaillé affiché |
| Suivi du poids corporel (courbe) | 🔲 | Entité DB existe, aucune UI |

---

## Paramètres

| Fonctionnalité | État | Notes |
|---|---|---|
| Lien vers Notifications | ✅ | |
| Filtres diététiques (porc, alcool, végétarien, vegan) | ✅ | Persistés en DataStore |
| Info mode halal | ✅ | Caveat certification |
| Export des données (JSON) | ✅ | Via sélecteur de fichier |
| Import / restauration des données | ✅ | Dialog de confirmation |
| Affichage version (À propos) | ✅ | Kalos 2.1.1 |

---

## Notifications & Rappels

| Fonctionnalité | État | Notes |
|---|---|---|
| Rappels intelligents (master switch) | ✅ | WorkManager daily |
| Rappel nutrition (si aucun repas enregistré) | ✅ | |
| Rappel activité physique (inactivité configurable 2/3/5j) | ✅ | |
| Rappel hydratation (si <50% objectif) | ✅ | |
| Heure d'envoi rappels intelligents (slider 6h-22h) | ✅ | |
| Rappels programme par séance (toggle par programme) | ✅ | |
| Heure d'envoi rappels programme | ✅ | |
| Rappel jour J / veille (par programme) | ✅ | Dans ProgramDetailScreen |

---

## Technique / Transverse

| Fonctionnalité | État | Notes |
|---|---|---|
| Mode sombre permanent | ✅ | |
| Persistance Room (offline-first) | ✅ | |
| Police Nunito (Google Fonts, downloadable) | ✅ | |
| Localisation française complète | ✅ | Dates, décimales, labels |
| Architecture MVVM + Repository + UseCases | ✅ | |
| Injection Hilt | ✅ | |
