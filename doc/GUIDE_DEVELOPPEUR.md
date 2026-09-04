# Guide développeur

> Pour la présentation du projet, les concepts et l'usage au quotidien (interface graphique et
> commande `lecture`), voir le [Guide utilisateur](GUIDE_UTILISATEUR.md).

## Stack technique

- **Langage** : Java 21, modules JPMS explicites (`module-info.java` dans `core` et `app`)
- **UI** : JavaFX 21.0.2 (`javafx-controls`, `javafx-fxml`) + [AtlantaFX](https://github.com/mkpaz/atlantafx)
  2.1.0 (thème Primer, clair/sombre)
- **Build** : Maven multi-module — voir [Architecture](#architecture) ci-dessous
- **Persistance** : JSON local (Jackson 2.17.2 + `jackson-datatype-jsr310`)
- **Tests** : JUnit 5.10.3 (`core` : modèle/persistance/CLI) + [TestFX](https://github.com/TestFX/TestFX)
  4.0.18 (`app` : parcours UI critiques — création de cours, création de créneau, undo/redo)
- **Ouverture de fichiers** : commande native du système (`xdg-open` sur Linux, `open` sur macOS,
  `start` sur Windows) via `ProcessBuilder`. `java.awt.Desktop` a été écarté : l'initialiser dans
  une appli JavaFX sur Linux charge un toolkit GTK concurrent de celui de JavaFX et fait planter
  la JVM.
- **Packaging** : `jpackage`/`jlink` (voir [Packaging et distribution](#packaging-et-distribution))

## Architecture

Projet Maven multi-module :

- **[core](../core/)** (module JPMS `com.courseflow.core`, packages `cli`/`model`/`persistence`/
  `io`/`util`) — **zéro dépendance JavaFX/AtlantaFX**. Contient le modèle de données, la
  persistance, et la commande CLI `lecture` en entier.
- **[app](../app/)** (module JPMS `com.courseflow.app`, packages `app`/`ui`) — interface
  graphique JavaFX, dépend de `core`.

Cette séparation existe pour une seule raison : que `lecture` n'ait **jamais** à résoudre le
module graphique au démarrage. Une commande CLI censée être quasi-instantanée ne doit pas payer
le coût d'initialisation de JavaFX/AtlantaFX. Toute la suite de l'architecture en découle,
jusqu'au détail du packaging (voir plus bas, `LanceurGraphiqueVoisin`) : rien de `javafx.*`/
`atlantafx.*` ne doit jamais fuiter dans `core`.

### Structure du projet

```
core/src/main/java/com/courseflow/
 ├─ model/         Cours, Fichier, Creneau, EmploiDuTemps, Parametres, PlageHoraire,
 │                  TypeSemaine, DossierReference — le modèle, sérialisable par Jackson
 ├─ persistance/    DataStore (lecture/écriture JSON atomique), DonneesIllisiblesException
 ├─ cli/            Lecture (point d'entrée), ArgumentsLecture (parsing), une classe par
 │                  sous-commande (CommandeOuverture, CommandesConsultation, CommandeSemaine),
 │                  helpers (OutilsLecture, NavigationCreneaux, GrilleAscii, AideLecture,
 │                  LanceurGraphiqueVoisin)
 ├─ io/             OuvreurFichiers — ouverture de fichiers via commande native, partagée
 │                  entre l'UI et la CLI
 └─ util/           TypeFichier (détection par extension), NomsJours (jours en français)

app/src/main/java/com/courseflow/
 ├─ app/            App — point d'entrée JavaFX, navigation, thème, chargement/sauvegarde
 └─ ui/             AccueilPane, CoursGestionPane, EmploiDuTempsPane, ParametresPane (les 4
                    écrans) + composants partagés (Icons, IconesFichier, Couleurs, TagPills, Toast)

bin/                Scripts de développement (lecture, donnees-test, generer-icone, build-installer)
packaging/          Icône source (SVG) et ressources de packaging (postinst/postrm .deb)
doc/                Cette documentation
```

### Modèle de données

```
Fichier          — chemin (ou URL), nom d'affichage optionnel, tags
Cours            — nom, couleur, fichiers possédés, dossiers référencés, fichiers "liés"
                    (IDs de fichiers empruntés à un autre cours)
Creneau          — jour, heure début/fin, coursId, salle/description optionnelles,
                    sous-ensemble de fichiers sélectionnés, TypeSemaine (TOUTES/A/B)
EmploiDuTemps    — racine sérialisée : liste de Cours + liste de Creneau + Parametres
Parametres       — jours affichés, pas de déplacement, plage horaire, blocs horaires,
                    vocabulaire de tags + couleurs, cours par défaut, thème, ancrage semaine A/B
DossierReference — chemin d'un dossier réel + fichiers déjà importés depuis lui
```

Les fichiers appartiennent au `Cours` (`Cours.fichiers`), jamais au `Creneau`, qui ne référence
que des IDs (`fichiersSelectionnesIds`) parmi ceux visibles pour son cours
(`EmploiDuTemps.fichiersVisibles`). Le lien entre cours (`Cours.fichiersLies`) est résolu en un
seul saut contre les fichiers réellement *possédés* par un cours
(`EmploiDuTemps.trouverFichierPartage`), jamais en suivant récursivement les liens d'un autre
cours — donc pas de risque de cycle malgré le croisement possible entre cours.

Le modèle est mutable, sans framework de state management : chaque pane JavaFX reçoit un
`Runnable surChangement` (passé en constructeur), déclenché après toute mutation, qui sauvegarde
tout l'`EmploiDuTemps` sur disque via `DataStore`.

## Installation et démarrage (développement)

### Prérequis
- JDK 21
- Maven 3.8+

### Lancer l'application
```
mvn -pl core -am install -DskipTests
mvn -f app/pom.xml javafx:run
```
(`-f app/pom.xml` est nécessaire : `mvn -pl app javafx:run` échoue avec « No plugin found for
prefix 'javafx' », la résolution du préfixe de plugin regarde le `pom.xml` du répertoire courant
— le parent, qui ne déclare pas `javafx-maven-plugin` — pas celui du module ciblé par `-pl`. La
première commande installe `core` dans le dépôt local Maven, nécessaire car `app` en dépend comme
d'un artefact normal une fois en dehors d'un build de reactor multi-module ; à refaire après une
modification de `core`.)

### Lancer les tests
```
mvn test
```
Couvre `core` (modèle, persistance, CLI) et, désormais, les parcours critiques de `app` (créer un
cours, créer un créneau, annuler/rétablir) pilotés via TestFX — voir
[Roadmap](#état-davancement--roadmap) pour l'état détaillé de la couverture. Les tests `app`
ouvrent de vraies fenêtres JavaFX : sur une machine sans écran (ex. un conteneur CI), lancer sous
`xvfb-run -a mvn test` (voir [.github/workflows/ci.yml](../.github/workflows/ci.yml)).

### Installer la commande `lecture` en mode dev
```
ln -s "$(pwd)/bin/lecture" ~/.local/bin/lecture
```
(`~/.local/bin` doit être dans le `PATH`.) `bin/lecture` tourne depuis le checkout source via
Maven — pour une installation native sans dépendance à Maven ni au checkout, voir
[Packaging et distribution](#packaging-et-distribution).

### Générer des données de test
```
bin/donnees-test
```
Génère 7 cours réalistes (niveau + matière, couleur aléatoire, 2-4 fichiers chacun) et 18
créneaux répartis sur la semaine, dans `~/.courseflow/data.json`. Les fichiers factices sont
créés dans [test/](../test/) (texte brut). Les données existantes sont sauvegardées en `.bak`
avant d'être remplacées.

Le script lance `java` directement sur le seul module `core` (module-path + classpath mis en
cache dans `core/target/lecture-classpath.txt` lors du premier appel, régénéré si `pom.xml`
change) plutôt que de repasser par `mvn javafx:run` à chaque fois — ~0,3 s par appel au lieu de
~1,4 s, Maven n'ayant plus à se réinitialiser. Seul `lecture .` (lancement de l'appli graphique)
passe encore par `mvn javafx:run` en dev, sans impact puisqu'il n'est pas sur le chemin critique
d'usage répété.

## Packaging et distribution

```
bin/build-installer
```
Compile, fait tourner les tests, puis construit dans `target/dist/` :
- un app-image portable (`target/dist/courseflow/`), utilisable sans installation — pratique
  pour tester ;
- un paquet `.deb` installable (`target/dist/courseflow_1.0.0_amd64.deb`).

Les deux embarquent leur propre image d'exécution Java (via `jlink`), avec deux points d'entrée
partageant le même runtime : `courseflow` (interface graphique, lanceur natif jpackage) et
`lecture` (CLI). `lecture` **n'est volontairement pas** le lanceur natif jpackage : ce dernier
interroge `rpm`/`dpkg` à chaque démarrage pour s'auto-identifier (~1 à 1,3 s perdues à chaque
lancement, mesuré au `strace` — inutile pour une commande CLI censée être instantanée). Il est
remplacé par un script qui appelle directement le runtime Java packagé sur le module
`com.courseflow.core` — qui ne dépend jamais de JavaFX/AtlantaFX — écrit dans
`target/dist/courseflow/bin/lecture` par [bin/build-installer](../bin/build-installer) pour
l'app-image, et dans `/opt/courseflow/bin/lecture-fast` par le `postinst` du `.deb` (voir plus
bas). Résultat : ~0,6 s par appel au lieu de ~1,8-2 s.

Installer le paquet :
```
sudo dpkg -i target/dist/courseflow_1.0.0_amd64.deb
```
Le paquet `.deb` relie automatiquement la commande dans `/usr/local/bin/lecture` (déjà dans le
`PATH` par défaut sur Debian/Ubuntu) via un script `postinst` personnalisé
([packaging/deb/postinst](../packaging/deb/postinst), retiré symétriquement par
[packaging/deb/postrm](../packaging/deb/postrm) à la désinstallation) — `lecture` est donc
utilisable immédiatement après `dpkg -i`, sans étape manuelle. Pour l'app-image portable
(`target/dist/courseflow/`, sans passer par `dpkg`), relier la commande manuellement (même
principe que l'installation en mode dev) :
```
ln -s "$(pwd)/target/dist/courseflow/bin/lecture" ~/.local/bin/lecture
```

`lecture .` fonctionne aussi depuis le binaire packagé : il démarre l'exécutable `courseflow`
voisin, détaché via `setsid`, avec les variables d'environnement propres au lanceur `lecture`
retirées pour ne pas perturber son démarrage (voir `LanceurGraphiqueVoisin`
[dans core/cli](../core/src/main/java/com/courseflow/cli/LanceurGraphiqueVoisin.java)).

Nécessite un JDK complet (pas juste un JRE) pour `jpackage`/`jlink` ; `.deb` uniquement pour
l'instant (pas de `.rpm`, pas de build Windows/macOS — voir les pistes correspondantes dans
[BILAN_ET_PISTES.md](BILAN_ET_PISTES.md#10-pistes--packaging-et-déploiement-multiplateforme)).

### Icône

L'icône source est vectorielle : [packaging/icon/courseflow.svg](../packaging/icon/courseflow.svg)
(dossier + horloge, indigo `#4338CA` / ambre `#F59E0B`). Après toute modification, régénérer les
PNG dérivés :
```
bin/generer-icone
```
Le script rasterise le SVG (via ImageMagick `convert`) en `packaging/icon/courseflow.png`
(512×512, utilisé par `jpackage --icon` pour le lanceur natif) et en
`app/src/main/resources/com/courseflow/app/icon.png` (icône de fenêtre / barre des tâches,
chargée dans [App.java](../app/src/main/java/com/courseflow/app/App.java)). Les deux PNG sont
versionnés pour que le build n'ait pas besoin d'ImageMagick.

**Charte graphique** : l'accent de l'interface reprend l'indigo de l'icône. Les fichiers
[charte-claire.css](../app/src/main/resources/com/courseflow/app/charte-claire.css) et
[charte-sombre.css](../app/src/main/resources/com/courseflow/app/charte-sombre.css) surchargent
la rampe `-color-accent-*` d'AtlantaFX (thème Primer) et sont chargés en plus du thème par
`App.appliquerTheme()`.

## État d'avancement / Roadmap

### Vue d'ensemble

- **Socle (modèle + persistance)** ✅ — [Fichier](../core/src/main/java/com/courseflow/model/Fichier.java),
  [Cours](../core/src/main/java/com/courseflow/model/Cours.java),
  [Creneau](../core/src/main/java/com/courseflow/model/Creneau.java),
  [EmploiDuTemps](../core/src/main/java/com/courseflow/model/EmploiDuTemps.java) ; persistance
  JSON via [DataStore](../core/src/main/java/com/courseflow/persistence/DataStore.java) (Jackson)
  dans `~/.courseflow/data.json` ; couvert par des tests unitaires (`mvn test`). Écriture atomique
  (fichier `.tmp` + renommage) et copies de secours rotatives (`data.json.bak1`..`bak3`) pour
  qu'une interruption ne tronque jamais l'unique fichier ; un `data.json` présent mais illisible
  est mis de côté (`.corrompu-<horodatage>`) plutôt qu'ignoré silencieusement. Champs JSON
  inconnus tolérés (compat ascendante).
- **Gestion des Cours (UI)** ✅ — [CoursGestionPane](../app/src/main/java/com/courseflow/ui/CoursGestionPane.java) :
  créer/renommer/colorer un cours, lui attacher des fichiers (sélection individuelle, dossier réel
  référencé et actualisable, ou lien web) ou en retirer, supprimer un cours (icône corbeille).
  Recherche/filtre par nom ou tag ; étiquettes choisies dans un vocabulaire prédéfini/extensible ;
  liaison de fichiers depuis n'importe quel autre cours. Création/renommage de cours couverts par
  [CoursGestionPaneTest](../app/src/test/java/com/courseflow/ui/CoursGestionPaneTest.java) (TestFX),
  le reste testé manuellement dans l'application.
- **Emploi du temps (UI)** ✅ — [EmploiDuTempsPane](../app/src/main/java/com/courseflow/ui/EmploiDuTempsPane.java) :
  grille des 7 jours, créer/modifier/supprimer un créneau via une boîte de dialogue, rendu coloré
  par cours. Un créneau peut être glissé/redimensionné à la souris ou déplacé/ouvert/supprimé au
  clavier. La largeur des colonnes s'adapte à la largeur de la fenêtre. Création de créneau et
  undo/redo couverts par
  [EmploiDuTempsPaneTest](../app/src/test/java/com/courseflow/ui/EmploiDuTempsPaneTest.java)
  (TestFX), le reste testé manuellement.
- **Sélection de fichiers par créneau (UI)** ✅ — liste à cocher des fichiers du Cours associé
  (tout coché par défaut à la création), boutons "Tout cocher"/"Tout décocher", bouton
  "Ouvrir maintenant". Testé manuellement.
- **Commande `lecture` (CLI)** ✅ — [Lecture](../core/src/main/java/com/courseflow/cli/Lecture.java) +
  [ArgumentsLecture](../core/src/main/java/com/courseflow/cli/ArgumentsLecture.java) +
  [NavigationCreneaux](../core/src/main/java/com/courseflow/cli/NavigationCreneaux.java) +
  [GrilleAscii](../core/src/main/java/com/courseflow/cli/GrilleAscii.java) (testés
  unitairement) : sous-commandes `slot`/`slots`/`schedule`/`courses`/`course`/`open-file`/`week`,
  ciblage par `--day`/`--date`+`--time` ou `--next`/`--previous`, lancement de l'appli graphique
  (`lecture .`). Vit entièrement dans `core` (zéro dépendance JavaFX/AtlantaFX). Testé
  manuellement en ligne de commande.
- **Accueil (UI)** ✅ — [AccueilPane](../app/src/main/java/com/courseflow/ui/AccueilPane.java) :
  équivalent graphique de la commande `lecture`, affiche le créneau du moment avec navigation
  précédent/suivant. Onglet par défaut au lancement.
- **Thème (UI)** ✅ — AtlantaFX (Primer) appliqué au démarrage ; case "Thème sombre" dans la barre
  latérale, préférence persistée. Couleurs de chrome sur jetons du thème ; couleurs par cours
  codées en dur (choisies par l'utilisateur, hors thème).
- **Reste à construire** : fonctionnalités avancées et packaging natif complet (Phases 7-8,
  optionnelles) — voir aussi [BILAN_ET_PISTES.md](BILAN_ET_PISTES.md) pour un inventaire plus
  large et plus spéculatif que cette roadmap.

### Détail par phase

#### Phase 0 — Cadrage ✅
- [x] Définir le concept et le modèle de données
- [x] Choisir le mécanisme de persistance → JSON local
- [x] Décider du format d'invocation de `lecture` → script bash dans `~/.local/bin` appelant le jar
- [ ] Esquisser les écrans principaux (wireframes rapides) — fait au fil de l'implémentation

#### Phase 1 — Modèle de données & persistance ✅
- [x] Implémentation des classes `Cours`, `Fichier`, `Créneau`, `EmploiDuTemps`
- [x] Sérialisation/désérialisation JSON (Jackson)
- [x] Emplacement de stockage des données utilisateur (`~/.courseflow/data.json`)
- [x] Tests unitaires de validation (modèle + persistance)

#### Phase 2 — Gestion des Cours ✅
- [x] Écran de création/renommage/couleur d'un Cours
- [x] Ajout de fichiers (sélecteur natif, individuel ou multiple) et import d'un dossier
- [x] Suppression de fichiers et suppression d'un Cours (avec confirmation, cascade sur créneaux)
- [x] Liste/vue d'ensemble des Cours existants

#### Phase 3 — Emploi du temps ✅
- [x] Grille des 7 jours (7h-20h en lignes)
- [x] Assignation d'un Cours à un créneau (création/édition/suppression via boîte de dialogue)
- [x] Vue visuelle claire (couleur du cours, nom, horaires)
- [x] Déplacement/redimensionnement d'un créneau à la souris
- [x] Largeur des colonnes adaptée à la largeur de la fenêtre

#### Phase 4 — Sélection de fichiers par créneau ✅
- [x] Interface de sélection des fichiers à utiliser parmi ceux du Cours associé
- [x] Distinction visuelle "fichiers du cours" / "fichiers sélectionnés pour cette séance"
- [x] Bouton "ouvrir maintenant" depuis l'interface graphique

#### Phase 5 — Commande `lecture` ✅
- [x] Mode d'exécution headless / point d'entrée CLI dédié
- [x] Détection automatique du créneau courant
- [x] Ouverture des fichiers associés via une commande native
- [x] Gestion des cas limites (aucun créneau, fichier non ouvrable, plusieurs fichiers)
- [x] Ciblage d'un autre créneau (`--day`/`--date`, `--time`)
- [x] `slots` liste les créneaux d'un jour
- [x] `--next`/`--previous` (navigation circulaire, avertissement si jour différent)
- [x] `slot` affiche les informations du créneau résolu
- [x] `schedule` affiche l'emploi du temps de la semaine en tableau ASCII
- [x] `courses`/`course`/`open-file`
- [x] `lecture .` lance l'application graphique, détachée du terminal
- [x] Script d'installation ([bin/lecture](../bin/lecture))

#### Phase 6 — Confort & robustesse (quasi terminée — 2 items reportés)
- [x] Gestion des erreurs (fichier manquant, chemin invalide) avec message clair
- [x] Annuler/rétablir (Ctrl+Z / Ctrl+Maj+Z) sur l'emploi du temps
- [x] Édition rapide — glisser-déposer de fichiers, dupliquer un créneau
- [ ] Édition rapide — réorganisation des fichiers dans la liste d'un cours, duplication d'un
      jour entier (mis de côté pour l'instant)
- [x] Recherche/filtre dans la liste des cours et fichiers
- [x] Étiquettes/tags sur les fichiers
- [ ] Aperçu rapide d'un fichier (PDF/image) directement dans l'appli (mis de côté — aurait
      nécessité une nouvelle dépendance pour le rendu PDF)
- [x] Sauvegarde/export de la configuration
- [x] Dossier réel du disque référencé par un Cours, actualisable
- [x] Lien de fichiers entre cours : un cours peut lier des fichiers depuis n'importe quel autre
      cours existant, pas seulement un unique "cours par défaut" — résolution toujours à un seul
      saut contre les fichiers réellement *possédés*, donc pas de risque de cycle
- [x] Ajout de liens web (URL) comme "fichiers" ouvrables
- [x] Suppression d'un Cours via la même icône corbeille que la suppression de fichier
- [x] Refonte graphique professionnelle (AtlantaFX, clair/sombre, micro-interactions)
- [x] Section "Accueil" dans la barre latérale, onglet par défaut au lancement

#### Phase 7 — Fonctionnalités avancées (optionnel, post-MVP)
- [x] Gestion de semaines alternées (semaine A / semaine B), avec date d'ancrage réglable,
      sélecteur de semaine, filtrage automatique dans l'Accueil et le CLI. La navigation
      précédent/suivant ignore volontairement l'alternance pour l'instant.
- [ ] Cycle de N semaines nommées (au-delà de la simple alternance A/B)
- [ ] Notifications/rappels avant le début d'un créneau
- [ ] Historique ou statistiques d'usage
- [ ] Synchronisation multi-appareils

#### Phase 8 — Packaging & distribution
- [x] Génération d'installeurs natifs via `jpackage` (Linux) — app-image + `.deb`
- [x] `lecture` sans le coût du lanceur natif jpackage (~1,8-2 s → ~0,6 s par appel)
- [x] Exposition de la commande `lecture` dans le `PATH` (automatique pour le `.deb`)
- [x] Documentation d'installation et de mise à jour
- [~] Icône de l'application — placeholder SVG en place, à remplacer par le design définitif
- [ ] `.rpm`, build Windows/macOS (voir [BILAN_ET_PISTES.md](BILAN_ET_PISTES.md#10-pistes--packaging-et-déploiement-multiplateforme))

### Prochaine étape suggérée

Le parcours principal (Phases 0-5) est complet : créer des cours, les remplir de fichiers, les
placer dans l'emploi du temps, choisir les fichiers par séance, et les ouvrir en une commande.
Les phases restantes (6-8) sont des améliorations optionnelles de confort, de fonctionnalités et
de packaging — à prioriser selon l'usage réel de l'application plutôt qu'en amont. Pour un
inventaire plus large (au-delà des phases déjà cadrées ici), voir
[BILAN_ET_PISTES.md](BILAN_ET_PISTES.md).
