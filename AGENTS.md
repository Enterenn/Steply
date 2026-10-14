# AGENTS.md — Règles du projet Android (Kotlin / Jetpack Compose)

## Priorités
Réponds en français. Priorités : résultat fiable, coût minimal, peu d’itérations et diff minimal. Sois autonome sur les tâches simples. Arrête-toi dès qu’un arbitrage humain est nécessaire.

## 1. Anti-boucle et consommation — PRIORITÉ ABSOLUE
- Même erreur 2 fois de suite : STOP. Résume la cause probable, les tentatives et 1 à 2 options, puis attends ma réponse.
- Maximum 3 tentatives de correction par problème, y compris si les erreurs changent mais relèvent de la même cause racine.
- Ne relance jamais une commande identique sans changement de code, de paramètres ou d’hypothèse.
- Après chaque échec, indique brièvement l’information nouvelle obtenue et justifie la prochaine action. Sans piste vérifiable nouvelle : STOP.
- Maximum 8 commandes de diagnostic/vérification par tâche. Au-delà, explique pourquoi et demande mon accord. Les lectures ciblées, recherches simples et modifications ne comptent pas.
- Évite les recherches globales, relectures et diagnostics répétés sans raison précise. Limite les sorties volumineuses à la source.
- Si le contexte devient lourd ou si tu tournes en rond, STOP et propose un résumé court pour reprendre dans une nouvelle tâche.
- Ne corrige pas les erreurs ou avertissements hors périmètre : signale-les en une ligne.
- Ces règles sont des consignes de comportement, pas une limite technique de tokens ou de dépenses.

## 2. Fichiers et modifications
- Repère d’abord les fichiers et symboles concernés, puis lis uniquement les zones utiles. Ne modifie jamais un fichier sans avoir lu la partie concernée.
- Privilégie les modifications ciblées. Ne réécris pas entièrement un fichier existant pour contourner une modification ciblée qui échoue : relis la zone et réessaie une fois.
- N’écris entièrement un fichier existant que si sa taille est inférieure à 50 lignes ou si une refonte complète a été explicitement demandée.
- Pour un fichier de plus de 300 lignes à modifier lourdement, propose un découpage avant d’agir.
- Diff minimal : aucun reformatage, renommage, réorganisation d’imports ou refactoring non demandé.
- Ne supprime ni ne remplace du code fonctionnel sans nécessité justifiée.
- Évite de lire les dossiers et fichiers générés ou volumineux : `build/`, `.gradle/`, `local.properties`, les APK/AAB, rapports et logs volumineux. Ne consulte les fichiers générés que si la tâche l’exige précisément.
- Ne modifie pas les fichiers générés (par exemple code généré par KSP/KAPT) à la main.

## 3. Vérifications et commandes
- Après une modification Kotlin, lance en priorité une compilation ou vérification Gradle ciblée et pertinente, en réutilisant la configuration existante. Par exemple : `./gradlew :app:compileDebugKotlin` si le module s’appelle `app` et si cette tâche existe.
- Pour les tests, lance uniquement ceux liés à la modification, par exemple `./gradlew :app:testDebugUnitTest --tests 'com.example.MonTest'` si le module, la tâche et le filtre sont compatibles. Ne lance pas toute la suite sans nécessité ou demande.
- Pour les changements de ressources ou de configuration, choisis la vérification la plus limitée qui couvre réellement le changement.
- Ne lance pas de build complet, de test instrumenté, d’émulateur ou l’application sans nécessité réelle. Je teste l’application moi-même et je transmets les erreurs.
- Ne lance pas de synchronisation Gradle, de mise à jour des dépendances ou de changement de version sans raison. Ne modifie pas les fichiers de verrouillage ou de versions indirectement sans nécessité.
- Aucune nouvelle dépendance, plugin Gradle ou bibliothèque sans présenter son nom, sa raison et une alternative, puis obtenir mon accord.
- Filtre les sorties de commandes et n’affiche pas les logs complets inutilement.
- Avant de terminer, examine le diff et signale les fichiers inattendus ou changements collatéraux. Ne les supprime ni ne les restaure automatiquement.

## 4. Planification et autonomie
- Tâche simple, claire et limitée : exécute directement.
- Plus de 3 fichiers, changement d’architecture, de structure ou de modèle de données : propose un plan de 3 à 6 lignes et attends ma validation.
- Demande ambiguë : pose une question courte plutôt que de supposer.
- Si plusieurs approches sont réellement différentes, présente 2 options en une ligne chacune et recommande la meilleure.
- Ne lance pas plusieurs agents en parallèle sur les mêmes fichiers. Répartis les tâches uniquement si elles sont indépendantes et si le gain est clair.

## 5. Git — sécurité & Gitflow
- Le projet doit suivre la méthode **Gitflow**.
- Chaque nouvelle feature doit être développée dans une branche de type `feature/nom_de_la_feature` tirée depuis la branche `develop`.
- Au début d’une tâche de modification, vérifie `git branch --show-current` et `git status --short` si le dépôt Git est disponible.
- Si la branche est `main` ou si l’arbre de travail est sale, STOP et demande mon accord avant de modifier quoi que ce soit.
- Une branche par tâche. Ne travaille jamais directement sur `main`.
- **Validation obligatoire :** Tu dois obtenir ma validation avant chaque `git commit` et avant chaque fusion (`merge`).
- Aucun `git commit` ni `git push` sans accord explicite.
- Aucun `git reset`, `checkout`, `restore`, `stash`, `clean`, `rebase`, `merge` ou `branch -D` sans accord explicite. Ne détruis jamais les changements existants.
- **Langue des commits :** Tous les messages de commit doivent être rédigés en anglais.

## 6. Kotlin, Android et Jetpack Compose
- Respecte en priorité l’architecture, les conventions, la navigation, l’injection de dépendances et la gestion d’état déjà présentes. N’introduis pas de nouvelle architecture ou bibliothèque sans accord.
- Utilise Kotlin idiomatique, la null-safety et les API Android/Compose déjà compatibles avec les versions configurées dans le projet.
- Pour l’interface, privilégie les fonctions `@Composable` petites, lisibles et réutilisables. Sépare l’UI de la logique métier lorsque cela correspond à l’architecture existante.
- Respecte les principes Compose : état hoisté lorsque pertinent, flux d’état unidirectionnel, stabilité des paramètres et `remember`/`rememberSaveable` uniquement lorsque nécessaires.
- Évite les effets de bord dans le corps des composables. Utilise les API d’effets Compose adaptées (`LaunchedEffect`, `DisposableEffect`, etc.) uniquement si le cas le justifie.
- Respecte les conventions Material 3 et le système de thème existant. Ne crée pas de couleurs, dimensions, chaînes ou styles en dur si le projet dispose déjà de ressources ou de tokens de thème appropriés.
- Respecte les couches et contrats existants (par exemple UI, domaine, données, ViewModel et repositories) sans imposer ces couches si le projet ne les utilise pas.
- N’ajoute pas de commentaires évidents. Commente uniquement les décisions ou logiques métier difficiles à comprendre.
- Ajoute des tests unitaires pour la logique critique (règles métier, calculs, parsing, transformation d’état). N’ajoute pas de tests disproportionnés pour de simples éléments d’interface.
- N’invente pas de versions de SDK, de dépendances, de plugins ou de conventions : vérifie les fichiers Gradle et la structure du projet avant de t’appuyer dessus.

## 7. Documentation & Communication
- Le fichier `README` du projet doit impérativement être rédigé en **anglais**.
- Pour une tâche courante : réponses courtes, pas de théorie, pas de récapitulatif de la demande ni d’explication du code écrit.
- En fin de tâche : 5 lignes maximum — fichiers modifiés, changements, vérifications effectuées, risques ou reste à faire.
- Ne recopie pas dans le chat le code déjà écrit dans les fichiers.
- Sois détaillé pour les questions, explications, comparaisons, revues, blocages ou décisions d’architecture.
- Si une demande présente un risque ou une mauvaise approche, signale-le en 1 à 2 phrases avant d’agir.