# Audit — Scanner de code-barres alimentaire

> Date : 4 June 2026 · app v3.12.0
> Objectif : évaluer l'intégration d'un scan de code-barres pour ajouter un aliment, proprement, sans casser l'architecture existante.
> **Statut : IMPLÉMENTÉ en v3.14.0** — 3 phases livrées (data / décodage local ML Kit bundlé / résolution OpenFoodFacts opportuniste). Décisions retenues : réseau accepté, ML Kit bundlé, écran de validation pré-rempli sur hit OFF. Le reste du document est conservé comme trace de conception.

---

## Résumé exécutif

Le scan de code-barres est une feature à forte valeur (ajout d'aliment en 2 s au lieu d'une recherche manuelle), mais elle entre en **collision frontale avec le principe fondateur de Kalos : l'offline-first total**. Aujourd'hui l'app n'a **aucune permission réseau, aucune permission caméra, aucune dépendance réseau ou ML**. Scanner un code-barres implique les deux : la caméra (décodage) et, presque toujours, le réseau (résolution code-barres → aliment via une base externe).

La vraie décision n'est pas technique (les libs sont matures) mais **produit** : jusqu'où accepte-t-on d'introduire du réseau et une dépendance Google Play Services dans une app qui n'en a pas ?

**Recommandation en une phrase** : intégrer le scan comme un **enrichissement optionnel et dégradable** — décodage local via ML Kit, résolution via OpenFoodFacts avec cache local persistant, et échec gracieux (bascule sur création manuelle pré-remplie du code-barres) quand il n'y a pas de réseau. L'app reste utilisable à 100% sans jamais scanner.

---

## Ce que le scan requiert réellement

Trois briques distinctes, à décider séparément :

1. **Capture caméra** — permission `CAMERA` + une preview (CameraX).
2. **Décodage du code-barres** — transformer l'image en chaîne EAN-13/UPC (ML Kit ou ZXing).
3. **Résolution code-barres → aliment** — transformer "3017620422003" en macros. C'est la brique qui impose le réseau.

Les briques 1-2 sont purement locales et sans controverse. La brique 3 est le cœur du sujet.

---

## Brique 2 — décodage : options

| Option | Poids APK | Dépendance GMS | Offline | Verdict |
|---|---|---|---|---|
| **ML Kit Barcode (bundled)** | +~3-4 Mo | Non (modèle embarqué) | Oui | **Recommandé** — pas de dépendance Play Services, fonctionne sur tout device |
| ML Kit Barcode (unbundled, via GMS) | +~200 Ko | Oui (télécharge le modèle) | Non (1er usage) | À éviter — réintroduit une dépendance GMS + un download runtime |
| Google code scanner (`play-services-code-scanner`) | léger | Oui (GMS obligatoire) | Non | UI clé en main mais dépendance GMS forte |
| ZXing (`zxing-android-embedded`) | +~1 Mo | Non | Oui | Fonctionne, mais décodage moins robuste que ML Kit sur codes abîmés/faible lumière, UI datée |

**Reco brique 2** : **ML Kit Barcode Scanning en mode bundled** (`com.google.mlkit:barcode-scanning`). Pas de dépendance Google Play Services, modèle embarqué → cohérent avec l'esprit offline. Coût : ~3-4 Mo d'APK (l'app est aujourd'hui très légère, c'est le vrai prix à payer).

**Caméra** : **CameraX** (`androidx.camera:*`) pour la preview + l'analyse d'image, standard, intégrable en Compose via `AndroidView` ou `PreviewView`.

---

## Brique 3 — résolution : le vrai arbitrage

Un code-barres seul ne dit rien des macros. Il faut une base de correspondance. Trois stratégies :

### Option A — OpenFoodFacts en ligne (API REST)
- `https://world.openfoodfacts.org/api/v2/product/{barcode}.json` → nom, marque, macros pour 100 g.
- **Pour** : base gratuite, ~3M de produits, couverture FR excellente, pas de clé API.
- **Contre** : impose `INTERNET` + une lib réseau (aucune aujourd'hui). Casse l'offline-first si non encadré. Données parfois incomplètes/incohérentes (base communautaire) → validation nécessaire avant insertion.
- **Nécessite** : un client HTTP. Le plus léger cohérent avec la stack = **Ktor client** ou **une simple `HttpURLConnection`** maison (zéro dépendance). Vu qu'on ne fait qu'un GET JSON ponctuel, `HttpURLConnection` + `kotlinx.serialization` (déjà présent) évite d'ajouter Retrofit/OkHttp.

### Option B — sous-ensemble OpenFoodFacts embarqué (offline pur)
- Bundler un dump filtré (produits FR populaires) dans les assets.
- **Pour** : garde l'offline-first intact.
- **Contre** : le dump OFF complet fait plusieurs Go ; même filtré aux produits FR avec code-barres, on parle de centaines de Mo → APK inacceptable. Périmé dès le build. **Rejeté.**

### Option C — hybride (recommandé)
- Décodage local (brique 2, offline).
- Lookup réseau OpenFoodFacts **si** connectivité, sinon échec gracieux.
- **Cache local persistant** : tout produit résolu est enregistré comme `FoodEntity` (avec son `barcode`) → le 2e scan du même produit est instantané et offline. Le cache se construit à l'usage.
- Sans réseau ET code-barres inconnu → on ouvre `CustomFoodScreen` **pré-rempli avec le code-barres**, l'utilisateur saisit les macros manuellement (une fois), et le produit devient réutilisable.

**Reco brique 3 : Option C.** Elle respecte l'offline-first (l'app ne dépend jamais du réseau pour fonctionner), n'ajoute le réseau que comme accélérateur opportuniste, et transforme chaque scan en donnée locale réutilisable.

---

## Impact sur le modèle de données

- **`FoodEntity` : ajouter `val barcode: String? = null`** + index. Migration Room **v14 → v15** (`ALTER TABLE food ADD COLUMN barcode TEXT`). Non destructif, `DEFAULT NULL`.
- Nouvelle requête `FoodDao.findByBarcode(barcode: String): FoodEntity?` — vérifie le cache local avant tout appel réseau.
- `FoodBackup` : ajouter `barcode: String? = null` (default → compat backups existants, comme les champs ajoutés en v3.7.0).
- Mapping OpenFoodFacts → `FoodEntity` : `product_name` → name, `brands` → brand, `nutriments.energy-kcal_100g` → kcalPer100g, `proteins_100g`, `carbohydrates_100g`, `fat_100g`, `fiber_100g`, `sugars_100g`. **Validation obligatoire** : rejeter les produits sans macros exploitables (OFF a beaucoup d'entrées vides) et le signaler à l'utilisateur plutôt que d'insérer des zéros.

---

## Permissions & manifest

- `CAMERA` (obligatoire, runtime permission — flux de demande + fallback si refusée).
- `INTERNET` (pour l'option A/C). **C'est la première permission réseau de l'app** — impact sur la perception de confidentialité, à assumer explicitement (mention dans les paramètres / au premier scan).
- Pas de `ACCESS_NETWORK_STATE` strictement nécessaire mais utile pour détecter la connectivité avant de tenter le lookup (échec gracieux plus propre).

---

## Impact architectural (le point le plus sensible)

| Aspect | Aujourd'hui | Après scanner | Gravité |
|---|---|---|---|
| Permissions | POST_NOTIFICATIONS uniquement | + CAMERA + INTERNET | Structurant — l'app quitte le statut "zéro réseau" |
| Dépendances | Aucune lib réseau/caméra/ML | + ML Kit (~3-4 Mo), CameraX, éventuellement Ktor | Poids APK ×2-3 potentiel |
| Offline-first | Absolu | Conditionnel (dégradable) | Acceptable **si** l'échec gracieux est réellement implémenté |
| Modèle de repository | 100% local (Room + assets) | + une source réseau dans `FoodRepository` | Nouvelle couche `RemoteFoodDataSource` à isoler proprement |
| Confidentialité | Aucune donnée ne sort | Les codes-barres scannés sont envoyés à OpenFoodFacts | À documenter honnêtement (cohérent avec le ton de la carte halal) |

**Conclusion architecturale** : c'est faisable proprement, mais ce n'est pas un "petit ajout". C'est l'introduction d'une **couche réseau** dans une app qui en était vierge. Le design doit garantir que le réseau reste strictement optionnel et isolé (un `RemoteFoodDataSource` injecté, testable, mockable, jamais appelé sur le chemin critique d'un ajout manuel).

---

## Plan de mise en œuvre proposé (par phases, chacune livrable seule)

**Phase 1 — fondations data (offline, sans risque)**
- `FoodEntity.barcode` + migration v15 + `findByBarcode` + `FoodBackup.barcode`.
- Aucune UI, aucune permission. Prépare le terrain, testable isolément.

**Phase 2 — décodage local (caméra, sans réseau)**
- CameraX + ML Kit bundled, écran de scan, permission CAMERA.
- Résolution limitée au **cache local** (`findByBarcode`) : si le code-barres est déjà connu (aliment custom saisi manuellement avec ce code), on l'ajoute directement. Sinon → `CustomFoodScreen` pré-rempli du code-barres.
- **À ce stade, zéro réseau.** L'app reste offline. Déjà utile pour re-scanner ses propres produits.

**Phase 3 — résolution en ligne (réseau opportuniste)**
- `RemoteFoodDataSource` (OpenFoodFacts, `HttpURLConnection` + kotlinx.serialization, zéro nouvelle grosse dépendance).
- Permission INTERNET + détection de connectivité + échec gracieux.
- Validation stricte des macros avant insertion en cache.

Découper ainsi permet de livrer la valeur "re-scan de mes produits" (phase 2) sans jamais toucher au réseau, et de n'ouvrir le réseau (phase 3) que comme une décision produit séparée et réversible.

---

## Risques principaux

| Risque | Mitigation |
|---|---|
| Rupture de l'offline-first | Réseau strictement optionnel, isolé dans un data source, jamais sur le chemin critique. Échec gracieux → création manuelle |
| Poids APK ×2-3 (ML Kit bundled) | Assumé ; alternative ZXing (~1 Mo) si le poids devient bloquant, au prix de robustesse |
| Données OpenFoodFacts incomplètes/fausses | Validation avant insertion + écran de confirmation pré-scan (l'utilisateur voit les macros avant d'ajouter) |
| Perception confidentialité (1re sortie réseau) | Mention explicite au premier scan + dans les paramètres, ton honnête (cf. carte halal) |
| Permission CAMERA refusée | Fallback : message clair + accès direct à la création manuelle |
| Migration v15 sur base existante | `ADD COLUMN ... DEFAULT NULL` = la migration la plus sûre ; schéma exporté committé (déjà en place depuis v3.7.0) |

---

## Ce que je recommande de décider avant tout code

1. **Accepte-t-on d'introduire le réseau dans l'app ?** (Option C oui / offline pur non → alors phase 2 seulement, scan limité au cache local de ses propres produits.)
2. **Poids APK** : ML Kit bundled (~+4 Mo, offline, robuste) accepté ?
3. **Découpage** : phase 1+2 d'abord (offline, sûr), phase 3 (réseau) comme décision séparée ?
