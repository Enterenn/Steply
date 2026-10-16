# Steply — Plan de refonte (phase 2)

*Chantier ouvert après la phase 1 (base Kotlin native). Pas de deadline : qualité architecture et fiabilité avant build release.*

## 1. Objectif du chantier

Reprendre les **points forts d’Astra** (clarté des responsabilités, orchestration de la collecte, testabilité, fiabilité arrière-plan) **sans** réimporter :

- le moteur Flutter et les plugins ;
- le hub multi-capteurs (BLE, foreground service santé, pipeline d’ingestion lourd) ;
- la granularité excessive de dossiers pour un seul capteur téléphone ;
- tout discours ou feature « santé / diagnostic » — Steply reste une **app de data** (visibilité, export, purge).

**Android only** : assumé.

## 2. État de départ (fin phase 1)

| Zone | État |
| --- | --- |
| Capteur | `StepCounterReader` natif (`TYPE_STEP_COUNTER`) |
| Persistance | Room + DataStore |
| Arrière-plan | `WorkManager` périodique + compaction |
| UI | Jetpack Compose, design maison |
| Métier pur | Fonctions testées (`compactReadings`, `daySteps`, tendances, CSV, etc.) |
| DI | `AppContainer` manuel |
| Notif objectif | **Partiellement** : logique dans `AppContainer.considerGoalNotice`, helpers dans `SettingsStore` ; déclenchée surtout quand l’app est active (combine settings + goal + readings) |

**Dette principale** : `AppContainer` et plusieurs écrans concentrent trop de rôles ; collecte live / worker / notif objectif ne passent pas par un **même orchestrateur** ; pas de couche presentation dédiée ; release sans minification.

## 3. Principes directeurs

1. **Proportionnalité** — une abstraction seulement si elle réduit la duplication ou prépare une extension réaliste (ex. second capteur plus tard), pas « au cas où ».
2. **Métier pur isolé** — calculs et règles sans Android API dans un package explicite, tests unitaires sans Robolectric.
3. **Un chemin pour la collecte** — foreground et background appellent le même coordinateur.
4. **UI passive** — les Composables observent un état ; pas de logique métier dans les gros fichiers UI.
5. **Pas de nouvelle lib lourde** — pas de Hilt, pas de BLoC ; **ViewModel Compose** (`lifecycle-viewmodel-compose`) accepté comme standard léger.
6. **Transparence data, pas medical** — pas de « collection health », pas de scores diagnostiques ; export, empreinte, état collecte OK (déjà aligné cahier des charges).

## 4. Architecture cible (schéma)

```
ui/           → Composables, navigation, thème
ui/state/     → ViewModels (StateFlow UI, appels au domaine / services)
core/         → lifecycle app, permissions, composition root (AppContainer allégé)
domain/       → règles pures (pas, compaction, tendances, objectif du jour…) — déplacé depuis data/steps où pertinent
data/         → Room, DataStore, StepCounterReader, implémentations
data/steps/   → repository + worker (minces, délèguent au coordinateur)
```

`AppContainer` reste le **composition root** : construction des singletons, exposition des ViewModels factories ou des deps nécessaires. Il ne gère plus les boucles « objectif atteint » ni le détail du cycle de collecte.

## 5. Phases de travail

Chaque phase se termine par : compilation `:app:compileDebugKotlin`, tests unitaires concernés, critères « done » cochés. PRs petites recommandées.

---

### Phase A — Package `domain/` et frontière data

**But** : séparer clairement **règles** et **I/O**.

**Actions**

- Créer `domain/` et y déplacer (sans changer le comportement) les fonctions et types purement métier aujourd’hui dans `data/steps` : compaction, deltas, agrégations jour/mois, dérivés affichage (distance/kcal si calcul pur), règles objectif.
- Laisser dans `data/` : DAO, entities, `StepCounterReader`, stores DataStore, sérialisation CSV fichier.
- `StepCounterRepository` : orchestration I/O + cache ; appelle `domain` pour compaction et calculs.

**Done quand**

- Aucune logique de compaction/agrégation dupliquée hors `domain/`.
- Tests existants repointés vers `domain/` (packages only), verts.

**Hors scope** : renommer toutes les classes « pour le style ».

---

### Phase B — Orchestration collecte (`StepCollectionCoordinator`)

**But** : un seul endroit pour « lire capteur → persister → compacter », inspiré du rôle d’`BackgroundCollector` chez Astra, sans multi-sources.

**Actions**

- Introduire `StepCollectionCoordinator` (nom ajustable) avec responsabilités :
  - vérifier permission activité ;
  - `recordReading()` / ingestion d’un cumul ;
  - `compactStoredReadings()` ;
  - notifier le **service objectif** (phase C) après écriture pertinente.
- `StepCounterWorker` et `AppContainer.onAppForeground` / refresh live délèguent au coordinateur (plus de duplication worker vs UI).
- Documenter dans le code (court) le flux : live listener → debounce/dedup déjà dans repo → coordinator si besoin de factoriser.

**Done quand**

- Worker et foreground utilisent le même API de collecte.
- Comportement identique ou mieux (ex. compaction aussi après worker — déjà partiellement le cas).

**À ne pas faire** : interface `DataIngestionSource` + liste de sources tant qu’il n’y a que le capteur téléphone.

---

### Phase C — Notification objectif atteint (produit + technique)

**But** : notif **fiable** une fois par jour local quand l’objectif est atteint, app ouverte ou non, sans logique éparpillée dans `AppContainer`.

**Existant à consolider** : `considerGoalNotice`, `canPostGoalNotice`, `postGoalNotice`, préférences `goalNotifications` / `goalNotifiedOn`.

**Actions**

- Extraire un **`GoalReachedNotifier`** (ou `DailyGoalNotificationService`) :
  - entrée : `evaluate(now, stepsToday, goal, settings)` ;
  - idempotent : respect de `goalNotifiedOn` + mutex ;
  - appelée depuis le coordinateur (après enregistrement) **et** au retour foreground (refresh).
- Retirer la boucle `combine` lourde de `AppContainer` init ; le container expose le service au coordinateur / lifecycle.
- Vérifier le chemin **background** : après `StepCounterWorker`, si pas du jour ≥ objectif → notif (si permission POST_NOTIFICATIONS et opt-in).
- Tests unitaires : objectif non atteint, atteint, déjà notifié aujourd’hui, notifs désactivées, changement d’objectif en cours de journée (comportement à figer dans le doc de règles ci-dessous).

**Règles produit proposées [à valider lors de l’implémentation]**

- Une notification max par **jour civil local** quand `steps >= goal` et `goal > 0`.
- Si l’utilisateur désactive puis réactive les notifs le même jour : pas de second envoi si déjà `goalNotifiedOn == today`.
- Si l’objectif est **abaissé** après avoir déjà notifié : pas de re-notif ce jour-là.
- Pas de notif si permission refusée (comportement silencieux, pas de spam settings).

**Done quand**

- Scénario manuel documenté dans PR : objectif atteint app tuée → worker → notif.
- Tests unitaires du notifier sans Android (interface `Notifier` fake pour le poster).

**Hors scope** : rappels « il te reste X pas », streaks, gamification.

---

### Phase D — Lifecycle application

**But** : formaliser le comportement Astra-like `AppLifecycleCoordinator` en version minimale.

**Actions**

- Classe `AppLifecycleHandler` (ou équivalent) : `onForeground()` / `onBackground()` appelée depuis `MainActivity` / `ProcessLifecycleOwner`.
- Responsabilités : permission → enqueue work → start/stop live updates → refresh DB → déléguer au coordinateur une lecture ; pas de logique objectif ici (phase C).
- Matrice courte dans ce fichier ou commentaire : permission retirée, Doze, app tuée (seul WorkManager).

**Done quand**

- `AppContainer.onAppForeground/Background` supprimés ou réduits à du câblage vers le handler.

---

### Phase E — Couche presentation (ViewModels)

**But** : réduire la taille des écrans (>400–700 lignes) et clarifier l’état UI.

**Ordre suggéré** (impact / risque)

1. `StepsScreen` + sheet objectif  
2. `SettingsScreen`  
3. `OnboardingScreen`  
4. `TrendsScreen`, `DataScreen`, menu  
5. Découper `SteplyApp` (navigation, bottom bar, menu stack)

**Actions**

- Ajouter dépendance `lifecycle-viewmodel-compose` (version alignée BOM / AGP du projet).
- ViewModels : `StateFlow` immuable, actions via méthodes ; injection via factory sur `AppContainer`.
- Composables : `collectAsStateWithLifecycle` où pertinent.
- Extraire sous-composables **sans** créer 50 fichiers : decoupe par feature (progress, ruler, goal editor).

**Done quand**

- Aucun écran principal > ~350 lignes (objectif souple ; exception justifiée pour layout unique).
- `AppContainer` sans setters métier exposés directement aux Composables si évitable (passer par ViewModel).

**À ne pas faire** : MVI complet, générateurs, navigation Compose library obligatoire (navigation actuelle OK si lisible).

---

### Phase F — Qualité data layer

**But** : petites corrections architecture / perf sans feature creep.

**Actions**

- Supprimer `runBlocking` dans `SettingsStore` : snapshot initial async au boot ou `StateFlow` seed depuis DataStore.
- `TimeProvider` / horloge injectable (`Clock` system default) pour tests domain + notifier.
- Revoir cache repository (documenter invalidation) — pas de refonte lourde.

**Done quand**

- Pas de `runBlocking` sur le thread principal path.
- Au moins les tests notifier / domain utilisent une horloge fixe.

---

### Phase G — Release (après refacto fonctionnelle)

**But** : poids APK et obfuscation, sans mélanger avec les phases A–F.

**Actions**

- Activer `isMinifyEnabled` + shrink resources en release.
- Règles ProGuard/R8 : Room, WorkManager, Compose (templates AndroidX).
- Mesurer APK release vs debug ; noter dans README ou changelog interne.

**Done quand**

- `:app:assembleRelease` OK, smoke test install release.

---

## 6. Non-objectifs explicites (chantier refacto)

- BLE / capteurs externes / foreground service permanent  
- SQLCipher ou chiffrement applicatif base (cf. cahier des charges V0)  
- Diagnostics « santé collecte » type medical ou scoring  
- Migration vers Hilt, KMP, ou réécriture navigation  
- Parité ligne-de-code avec Astra (~27k LOC)  
- iOS  

## 7. Extension future (hors refacto, note seulement)

Si un second source de pas apparaît : introduire alors `StepReadingSource` (interface unique) et brancher le coordinateur — **ne pas** créer l’interface avant le besoin.

## 8. Suivi

| Phase | Statut | Notes |
| --- | --- | --- |
| A — domain | Fait | Package `domain/` et frontière data |
| B — coordinator | Fait | `StepCollectionCoordinator` ; worker et premier plan |
| C — notif objectif | À faire | Existant partiel dans AppContainer |
| D — lifecycle | À faire | |
| E — ViewModels | À faire | |
| F — data quality | À faire | |
| G — release | À faire | Après A–F |

Mettre à jour ce tableau à chaque PR merge sur `develop`.

## 9. Référence rapide Astra → Steply

| Idée Astra | Retenue dans Steply | Écart volontaire |
| --- | --- | --- |
| Composition root (`AppDependencies`) | `AppContainer` allégé | Pas de graph DI framework |
| `BackgroundCollector` | `StepCollectionCoordinator` | Une source, pas de normalizer multi-device |
| Cubits presentation | ViewModels + Compose | Pas flutter_bloc |
| `TimeProvider` | `Clock` injectable | |
| Repos settings / metrics | Stores DataStore existants | Pas de repo health metrics |
| Notif objectif | `GoalReachedNotifier` dédié | Pas de foreground service |
| Tests massifs | Tests ciblés domain + coordinator + notifier | Pas objectif 90+ fichiers |

---

*Document vivant — première rédaction : phase 2 refacto, post base native 0.6.x.*
