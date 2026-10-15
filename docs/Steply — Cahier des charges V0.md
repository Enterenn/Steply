# Steply — Cahier des charges V0

*Version 0.1 du cahier des charges · octobre 2026 · Auteur : Baptiste · Statut : brouillon à valider*

Légende utilisée dans tout le document : **\[Décidé\]** = acté dans nos échanges · **\[Proposé\]** = recommandation à valider · **\[À trancher\]** = question ouverte.

## 1. Contexte et objectif

Steply est la nouvelle version, réécrite de zéro, de l'application précédemment nommée Astra (Flutter, v0.15.3, Phase 0 validée en beta le 2026-06-08). Trois raisons de repartir sur une base saine : le changement de nom, le recentrage sur les téléphones dégooglisés (si l'OS est opaque, la souveraineté des données ne peut pas être garantie), et le choix d'une technologie native mieux adaptée aux parties les plus fragiles de l'app (capteur, arrière-plan, base de données).

**Promesse en une phrase \[Proposé\]** : *Steply, le compteur de pas local, sans compte et exportable, pour téléphones dégooglisés.*

**Nature du projet** : POC soigné, pas un produit industriel. L'objectif est un résultat crédible, sobre et fini, sans l'aspect « AI slop » : périmètre serré, design maison, code lisible, comportements vérifiables.

## 2. Décisions actées

| Sujet | Décision | Statut |
| --- | --- | --- |
| Nom | Steply (disponibilité à vérifier, voir §13) | Décidé |
| Technologie | Kotlin natif (Android) | Décidé |
| Plateformes | Android dégooglisé : GrapheneOS, LineageOS, /e/OS. iOS abandonné. Téléphones Linux hors périmètre V0 | Décidé (Linux : à rouvrir plus tard) |
| Interface | Design maison issu de Figma, **sans Material Design** | Décidé |
| Langues | Français et anglais conservés | Décidé |
| Données | Locales uniquement, sans compte, sans cloud | Décidé |
| Licence du code | Apache-2.0 (comme Astra) | Proposé |

## 3. Principes produit

Repris d'Astra, ajustés :

1. **La preuve plutôt que la promesse** : export, purge et empreinte de stockage sont des comportements vérifiables, pas des slogans. Corollaire : aucune formulation (README, description du dépôt, écran About) ne doit affirmer plus que ce que l'app fait réellement (ex. ne pas écrire « chiffré » tant que ce n'est pas le cas).
2. **Local d'abord** : l'appareil est la source de vérité.
3. **Aucun compte obligatoire.**
4. **UX calme** : pas de honte liée aux séries, pas d'optimisation de l'engagement.
5. **Minimisation** : des agrégats interprétables, pas de données brutes de capteur.
6. **Transparence** : empreinte, export, purge et état de la collecte sont visibles.
7. **Autonomie en arrière-plan** : la valeur s'accumule app fermée.
8. **Souveraineté de l'utilisateur** : export et ré-import CSV.
9. **My Data est une surface de premier rang.**

**Catégorie** : outil de visibilité comportementale (General Wellness). Steply n'est ni un dispositif médical, ni un outil de diagnostic, ni un score de « forme ».

## 4. Périmètre de la V0

### 4.1 Inclus

- Comptage de pas via le capteur matériel du téléphone
- Collecte en arrière-plan, sans dépendance aux services Google
- Stockage local (Room / SQLite) avec cycle de vie borné (sous-échantillonnage, maintenance)
- Trois onglets : **Steps**, **Trends**, **Menu** (Profil, My Data, Réglages, Help, À propos)
- Onboarding orienté confiance : permission d'activité, poids et taille facultatifs
- Objectif quotidien (8 000 par défaut) avec historique, modifiable depuis Steps
- Métriques dérivées calculées dans l'UI : distance, durée de marche, kcal
- Export / import CSV, purge complète des données
- Réglages : langue (FR/EN), unités (métrique/impérial), notifications d'objectif, thème (système/clair/sombre), 6 accents

### 4.2 Exclu de la V0

- Bracelet / objet connecté, BLE, protocole ADP (stub abandonné : à rouvrir en V1)
- Health Connect, services Google, iOS
- Cloud, comptes, analytique, SDK tiers
- Coaching, classements, scores opaques, séries (« streaks »)
- Chiffrement de la base (voir §7.4)

### 4.3 Vision long terme (hors cahier des charges)

Le bracelet propriétaire, le firmware, le PCB et la certification n'ont pas leur place dans ce document ni dans le README de l'app. Ils sont déplacés dans un document de vision séparé. L'app doit valoir le coup seule.

## 5. Exigences fonctionnelles

### 5.1 Onboarding

- **F-01** Trois étapes avec indicateur de progression : message de confiance, poids, taille.
- **F-02** Demande de la permission de reconnaissance d'activité, avec écran de refus et accès direct aux réglages système.
- **F-03** Poids et taille facultatifs, saisis avec le sélecteur à règle (unités kg/lb, cm/pouces). Une saisie numérique alternative existe pour l'accessibilité.
- **F-04** Aucun compte, aucun e-mail.

### 5.2 Steps (aujourd'hui)

- **F-10** Vue semaine avec sélecteur de jour.
- **F-11** Anneau de progression : pas du jour, objectif, appui pour modifier l'objectif.
- **F-12** Trois statistiques dérivées : kcal, distance, durée de marche.
- **F-13** Indicateur discret de santé de la collecte.
- **F-14** *\[À trancher\]* Compteur hebdomadaire « 3/7 » (jours d'objectif atteints) : à garder comme simple repère factuel, ou retirer pour rester cohérent avec « pas de gamification ».

### 5.3 Trends

- **F-20** Contrôle segmenté 7 jours / 30 jours, histogramme avec ligne d'objectif historique en pointillés.
- **F-21** *\[À trancher\]* Vue 12 mois (présente dans l'app Astra, absente du Figma).
- **F-22** Deux tuiles : moyenne de kcal et moyenne de pas par jour sur la période ; carte du jour record.

### 5.4 Menu

- **F-30** Liste groupée : Informations (Profil, Data), Autres (Réglages, Help, À propos). Pas d'Achievements.
- **F-31** **\[Décidé\]** Pas d'Achievements. Help est une page de FAQ pour aider à l'utilisation de l'app.
- **F-32** Corriger l'en-tête du menu du Figma (il affiche « My Data »).

### 5.5 Profil

- **F-40** Nom d'affichage, poids, taille, modifiables.

### 5.6 My Data

- **F-50** Carte « Background » : état de la collecte, dernière synchronisation.
- **F-51** Carte « Footprint » : nombre d'échantillons, taille de la base, dernière optimisation.
- **F-52** Export CSV, import CSV, suppression de toutes les données locales avec confirmation explicite.

### 5.7 Réglages

- **F-60** Unités : distance, poids, taille.
- **F-61** Notifications d'objectif (locales uniquement) avec interrupteur.
- **F-62** Thème : système / clair / sombre, et 6 couleurs d'accent.
- **F-63** Langue : français / anglais, indépendante des unités.

### 5.8 À propos

- **F-70** Nom, version, licence. L'icône est à redessiner (voir §8.5).

### 5.9 Collecte et données

- **F-80** Lecture du compteur de pas matériel, détection des redémarrages et des remises à zéro du compteur.
- **F-81** Découpage proportionnel des pas à cheval sur minuit (aucune perte, aucun double comptage).
- **F-82** Une seule voie d'écriture vers la base, transactions atomiques pour toute opération multi-lignes.
- **F-83** Cycle de vie : sous-échantillonnage (5 min → 1 h → 1 jour) et maintenance, pour rester sous 50 Mo par an.

## 6. Exigences non fonctionnelles

| Domaine | Exigence |
| --- | --- |
| Confidentialité | Aucune permission INTERNET dans le build de release. Test automatisé du manifeste. Aucune dépendance réseau ou analytique dans le pipeline de données. |
| Fiabilité | Aucune perte de pas lors d'un redémarrage, d'un changement de fuseau, d'un passage à minuit ou d'une interruption de la collecte. |
| Stockage | Moins de 50 Mo par an ; empreinte visible dans My Data. |
| Batterie | Aucun service permanent en premier plan si la lecture périodique suffit (à valider par le test technique, §12). |
| Accessibilité | Contraste AA (4,5:1 texte, 3:1 éléments d'interface), zones tactiles d'au moins 48 dp, rôles et libellés TalkBack sur chaque composant de base, respect de la taille de police système. |
| Internationalisation | FR et EN complets, pluriels corrects, formats de nombres et de dates locaux. |
| Compatibilité | Android dégooglisé (Pixel/GrapheneOS en référence, LineageOS, /e/OS). `minSdk` à fixer. |
| Maintenabilité | Dépôt sobre, dépendances justifiées, tests sur la logique de pas. |

## 7. Architecture technique

### 7.1 Pile technique \[Proposé\]

| Couche | Choix |
| --- | --- |
| Langage | Kotlin |
| UI | Jetpack Compose, **sans Material 3** (composants construits sur `foundation` et Canvas) |
| Architecture | ViewModel + StateFlow, couches `core` / `data` / `ui` |
| Injection | Manuelle au départ (Koin envisageable) \[À trancher\] |
| Base de données | Room (SQLite), migrations versionnées |
| Réglages | DataStore |
| Arrière-plan | WorkManager (lecture périodique du compteur) |
| Capteur | `TYPE_STEP_COUNTER` via SensorManager |
| Export / import | Storage Access Framework (aucune permission de stockage) |
| Notifications | Locales uniquement |
| i18n | `strings.xml` (EN par défaut) + `values-fr`, `LocaleManager`, `locales_config.xml` |
| Build | Gradle (Kotlin DSL), catalogue de versions |
| Qualité | JUnit, tests de captures d'écran (Paparazzi), detekt, ktlint, GitHub Actions |

### 7.2 Pipeline de données

```
Capteur matériel (compteur de pas cumulatif)
  → Lecteur (WorkManager périodique + à l'ouverture de l'app)
  → Normaliseur (deltas, redémarrage, remise à zéro, passage à minuit)
  → Collecteur (seule voie d'écriture)
  → Repository → Room
  → UI (Steps / Trends / My Data)
  → Maintenance (sous-échantillonnage, VACUUM)
```

Règles : horodatages en UTC avec `zone_offset` immuable par échantillon ; agrégats calculés dans le repository (pas dans les composants) ; aucune logique métier dans l'UI.

### 7.3 Modèle de données

Table principale `timeseries_samples` : un échantillon = un intervalle agrégé.

| Champ | Exemple |
| --- | --- |
| `type` / `unit` | `steps` / `count` |
| `resolution` | `5min`, `1hour`, `1d` |
| `provider` / `device_id` | `internal_phone` / `smartphone` |
| `start_time` / `end_time` | ISO 8601 UTC |
| `zone_offset` | `+02:00` |
| `value` | nombre de pas |

À évaluer pendant le test technique : une version plus simple (relevés bruts du compteur + agrégation à la lecture) pour réduire la machinerie de compaction.

### 7.4 Sécurité des données \[À trancher\]

Sur un téléphone dégooglisé avec chiffrement du stockage, la base est déjà protégée au repos. Le chiffrement applicatif (SQLCipher) apporte surtout une valeur marginale, alors que le chiffrement **facultatif des exports** (mot de passe) protège les données là où elles quittent l'appareil. Proposition : pas de SQLCipher en V0, export chiffré optionnel en V1, et une formulation exacte partout (pas de « chiffré » inexact).

## 8. Design system

### 8.1 Typographie

- **Figtree** pour l'interface, **Darker Grotesque** pour les chiffres (données). Polices libres, embarquées dans l'APK, sans téléchargement.
- Chiffres tabulaires pour les compteurs si la police le permet. Darker Grotesque est fine : à valider en petite taille.
- Échelle (tailles par rôle : display, titre, corps, légende) : \[À fournir depuis Figma\].

### 8.2 Couleurs (de base)

| Rôle | Clair | Sombre |
| --- | --- | --- |
| Fond | #F8F9FB | #101115 |
| Carte | #FFFFFF | #1A1D26 |
| Texte | #323337 | #C8C8D7 |

Gris, vert (validation) et rouge (erreur) : \[À fournir\]. Contraste mesuré du texte principal : environ 12:1 en clair, 10 à 11:1 en sombre.

### 8.3 Accents

| Accent | Clair | Sombre |
| --- | --- | --- |
| Rouge | #E16969 | #822828 |
| Orange | #EDA464 | #8A4324 |
| Vert | #71D086 | #2E753E |
| Bleu | #6EBAFF | #296093 |
| Violet | #9384F8 | #523C93 |
| Rose | #F693C2 | #933C65 |

### 8.4 Règle des deux rôles d'accent \[Proposé\]

Contrastes mesurés (WCAG) :

| Accent | Clair : texte blanc dessus | Clair : #323337 dessus | Sombre : blanc dessus | Sombre : accent clair sur carte |
| --- | --- | --- | --- | --- |
| Rouge | 3,3 | 3,9 | 9,2 | 5,2 |
| Orange | 2,1 | 6,1 | 7,2 | 8,1 |
| Vert | 1,9 | 6,7 | 5,6 | 8,9 |
| Bleu | 2,1 | 6,1 | 6,6 | 8,1 |
| Violet | 3,0 | 4,1 | 8,7 | 5,5 |
| Rose | 2,1 | 5,9 | 6,8 | 7,9 |

Constat : le texte blanc sur les accents clairs échoue partout (seuil 4,5) ; en sombre, les teintes sombres sont trop discrètes comme éléments graphiques sur la carte (1,8 à 3,0). D'où deux rôles :

1. **`accent`** (anneau, barres, indicateurs) : la **teinte claire** dans les deux thèmes (5 à 9:1 sur carte sombre).
2. **`accentFill`** (fonds portant du texte : boutons, onglet sélectionné) : clair → teinte claire avec texte #323337 ; sombre → teinte sombre avec texte blanc.

Point ouvert : en clair, le rouge (3,9) et le violet (4,1) restent sous 4,5 avec #323337. Soit on assombrit légèrement leur fond pour les boutons, soit on réserve le texte gras et grand à ces deux accents.

### 8.5 Icône et identité

L'icône actuelle (lettre « a » avec étincelle à quatre branches) est à redessiner : l'étincelle est devenue le symbole des fonctions d'IA, un mauvais signal pour une app locale et sans cloud. L'icône suit le nouveau nom (Steply), avec une version adaptative Android.

### 8.6 Composants de base (ordre de construction)

1. Thème (`AppTheme`) : couleurs sémantiques, typographie, espacements, rayons, exposés par `CompositionLocal`. 12 palettes (6 accents × 2 thèmes) générées depuis une table.
2. Scaffold avec barre d'onglets flottante (marge basse, gestes système, rôle d'onglet pour TalkBack).
3. Carte, ligne de liste groupée, contrôle segmenté, bouton pilule (primaire, secondaire, texte destructif), interrupteur.
4. Pastille de jour, tuile de statistique.
5. Anneau de progression, histogramme (Canvas).
6. Sélecteur à règle (défilement à aimantation, retour haptique, saisie numérique alternative).

Règle : chaque composant de base intègre dès sa création zone tactile de 48 dp, libellés d'accessibilité et contraste conforme.

### 8.7 Fidélité au Figma

Export des variables Figma en JSON, génération du code des tokens par script (Style Dictionary ou script maison), tests de captures d'écran comparés aux exports Figma. Les valeurs ne sont jamais recopiées à la main.

## 9. Données, export et import

- **Format** : CSV avec colonnes alignées sur le vocabulaire Open Wearables (vocabulaire uniquement, aucune dépendance à un serveur).
- **Import** : validation stricte du format ; ne jamais faire confiance aux identifiants externes (leçon d'un bug d'Astra) ; rapport d'erreurs lisible.
- **Migration depuis Astra** : l'export CSV d'Astra, importé dans Steply, sert de chemin de migration et de test de compatibilité.
- **Purge** : suppression complète avec confirmation explicite, irréversible, vérifiable dans My Data.

## 10. Distribution et licence

- **Canaux** : F-Droid, IzzyOnDroid, GitHub Releases + Obtainium. Aucune bibliothèque propriétaire ni service Google, pour rester compatible F-Droid.
- **Identifiant d'application (`applicationId`)** : à fixer **avant** de créer le dépôt, car il ne se change plus après publication. Une convention sans nom de domaine, par exemple `io.github.<compte>.steply`, est possible.
- **Licence** : Apache-2.0 pour le code. Marque Steply, identité visuelle et clés de signature : à confirmer comme propriétaires \[À trancher\].

## 11. Qualité et méthode de travail

- Dépôt sobre : code, README court, quelques documents ; les artefacts de planification vivent ailleurs.
- Un fichier `AGENTS.md` court (règles d'architecture, interdits, commandes), pour cadrer l'assistant de code.
- Aucune dépendance sans justification écrite (fichier `DEPENDENCIES.md`).
- Tests obligatoires sur la logique de pas (redémarrage, remise à zéro, minuit, fuseau).
- Relecture avant chaque commit ; vérification sur appareil réel pour la collecte.
- **Définition de « terminé »** : fonctionnel, accessible (contraste, TalkBack, 48 dp), traduit FR/EN, testé, fidèle au Figma, sans code mort.

## 12. Plan de réalisation \[Proposé\]

| Jalon | Contenu | Sortie attendue |
| --- | --- | --- |
| M0 Cadrage | Nom vérifié, `applicationId`, tokens Figma complets, décisions du §14 | Cahier des charges v1.0 |
| M1 Test technique | Lecture périodique du compteur matériel sur appareil réel (GrapheneOS), fréquence de lecture, résistance aux redémarrages | Décision sur l'architecture de collecte |
| M2 Design system | Thème, 12 palettes, composants de base, captures de référence | Galerie de composants |
| M3 Collecte et stockage | Lecteur, normaliseur, Room, cycle de vie | Pipeline testé |
| M4 Écrans principaux | Steps, Trends | Écrans fidèles au Figma |
| M5 Menu et réglages | Onboarding, Profil, My Data, Réglages, Help, À propos | Parcours complet |
| M6 Export, i18n | CSV, import, purge, FR/EN complet | Fonctions de souveraineté |
| M7 Beta | Checklist, F-Droid | Version 0.1.0 publiée |

Pas de dates à ce stade : l'ordre compte plus que le calendrier pour un projet solo.

## 13. Migration depuis Astra

| On reprend | On abandonne |
| --- | --- |
| Principes produit, page My Data, tokens UX | `flutter_bloc`, `workmanager` (plugin), `pedometer` (plugin) |
| Modèle de données (UTC + `zone_offset`, intervalles) | Verrouillage de base entre isolates |
| Format CSV export/import | Compaction et baselines si trop complexes |
| Leçons des bugs corrigés (redémarrage, minuit, transactions atomiques, import non fiable) | iOS |
| FR/EN (conversion des fichiers ARB en `strings.xml` par script) | Roadmap matérielle dans le README |

Disponibilité du nom : le nom Steply n'a pas été vérifié ici. À contrôler avant de figer l'identifiant : F-Droid, GitHub, bases de marques (INPI / EUIPO).

## 14. Points ouverts

| # | Question | Impact |
| --- | --- | --- |
| 2 | Compteur « 3/7 » sur Steps : garder ou retirer ? | F-14, cohérence « sans gamification » |
| 3 | Vue 12 mois dans Trends ? | F-21, cadre Figma à créer |
| 4 | Rouge et violet en clair : assombrir le fond ou texte gras et grand ? | Accessibilité des boutons |
| 5 | Chiffrement : exports chiffrés uniquement, ou SQLCipher aussi ? | §7.4 |
| 6 | Gris, vert, rouge, échelle typographique, espacements, rayons | Thème (M0) |
| 7 | Cadres sombres et variantes d'accent dans Figma | Thème, tests de captures |
| 8 | `minSdk` | Compatibilité, API disponibles |
| 9 | Injection de dépendances : manuelle ou Koin ? | Architecture |
| 10 | Nouvelle icône sans étincelle | Identité, F-70 |
| 11 | Disponibilité du nom et `applicationId` | Création du dépôt |
| 12 | Statut de la marque et de l'identité visuelle (propriétaires ?) | Licence |

## Annexe : correspondance Figma ↔ écrans

| Cadre Figma | Écran Steply |
| --- | --- |
| onboarding / Weight / Height | Onboarding (3 étapes) |
| Today | Steps |
| History | Trends |
| Menu | Menu |
| Data | My Data |
| Profil | Profil |
| Settings | Réglages |
| About | À propos |
