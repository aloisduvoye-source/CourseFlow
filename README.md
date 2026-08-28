# CourseFlow

CourseFlow est une application de bureau (JavaFX) qui organise les fichiers de cours d'un enseignant autour de son emploi du temps. L'objectif : ne plus jamais perdre de temps à chercher le bon PDF, le bon diaporama ou la bonne feuille d'exercices avant un cours — un seul clic (ou une commande) ouvre exactement les fichiers dont on a besoin, au bon moment.

## Le problème

Un enseignant réutilise en grande partie les mêmes supports d'une semaine sur l'autre pour un même cours (ex. "6e A - Mathématiques"), mais avec des variations ponctuelles selon la séance (contrôle cette semaine, exercice différent, document ajouté au dernier moment...). Aujourd'hui, retrouver et ouvrir ces fichiers avant chaque heure de cours est répétitif et chronophage.

## L'idée

1. On définit des **Cours** réutilisables (ex. "6e A - Mathématiques"), chacun avec sa propre bibliothèque de fichiers associés (cahier de texte, supports, exercices, corrections...).
2. On remplit un **emploi du temps** hebdomadaire en assignant un Cours à chaque créneau (jour + heure).
3. Pour chaque créneau précis, on choisit — parmi les fichiers du Cours — lesquels sont réellement utiles **cette séance-là** (pas besoin de rouvrir tout le dossier du cours à chaque fois).
4. Le jour J, une commande terminal (`lecture`) ouvre automatiquement les fichiers sélectionnés pour le créneau courant (ou un créneau donné), avec les applications par défaut du système.

## Concepts clés / modèle de données

```
Fichier
 ├─ chemin (path vers le fichier sur le disque)
 └─ nom d'affichage (label optionnel, ex. "Corrigé DS1")

Cours
 ├─ nom (ex. "6e A - Mathématiques")
 ├─ couleur (identification visuelle dans l'emploi du temps)
 └─ liste de Fichiers rattachés au cours (la "bibliothèque" du cours)

Créneau (CreneauHoraire)
 ├─ jour de la semaine
 ├─ heure de début / heure de fin
 ├─ Cours associé (référence)
 ├─ salle (optionnel)
 ├─ description (optionnel)
 └─ sous-ensemble de Fichiers sélectionnés pour CE créneau
    (sélectionnés parmi les fichiers du Cours associé — pas une nouvelle liste libre)

EmploiDuTemps
 └─ ensemble des Créneaux de la semaine (grille type "semaine type", récurrente)
```

Point important de la structure : les fichiers appartiennent au **Cours**, pas au créneau. Le créneau ne fait que *piocher* dans les fichiers du cours. Cela évite de dupliquer les mêmes fichiers d'une semaine à l'autre : on les attache une fois au Cours, et on affine juste la sélection semaine par semaine si besoin.

## La commande `lecture`

Lancée dans un terminal, `lecture` fonctionne par sous-commandes (`slot`, `slots`, `schedule`,
`courses`, `course`, `open-file`) et options longues (`--next`, `--previous`, `--date`,
`--day`, `--time`) :
- sans sous-commande : ouvre les fichiers du créneau ciblé (courant par défaut),
- `--day <nom>`/`--date <AAAA-MM-JJ>` + `--time HH:mm` : cible un créneau précis,
- `--next` / `--previous` : ouvre le créneau **suivant**/**précédent** dans la semaine plutôt que celui du moment (boucle : après le dernier créneau du Dimanche, `--next` revient au premier du Lundi, et inversement) — si ce créneau tombe un autre jour que la référence, un message le signale avant d'ouvrir les fichiers,
- `slot` : affiche les informations du créneau ciblé (cours, salle, description, fichiers) sans rien ouvrir — mêmes options de ciblage que ci-dessus,
- `slots` : liste tous les créneaux d'un jour (aujourd'hui par défaut, ou via `--day`/`--date`),
- `schedule` : affiche l'emploi du temps de la semaine entière sous forme de tableau ASCII dans le terminal (jour courant marqué d'un `*`) — un tableau à bordures, une colonne par jour, où chaque créneau est une boîte précédée de sa plage horaire, affichant le nom du cours en majuscules, la salle et la description si renseignées, puis un fichier par heure de durée (avec un `+N fichiers` si tous ne tiennent pas). Les boîtes d'un même jour s'empilent par ordre chronologique, alignées par rang, pas par heure absolue,
- `courses` : liste tous les cours (avec leur nombre de fichiers) ; `--missing-info` ne liste que ceux sans aucun créneau planifié,
- `course [NOM]` : affiche le détail d'un cours (couleur, bibliothèque de fichiers complète, créneaux où il est programmé),
- `open-file --file [NOM]` : ouvre un fichier précis, soit dans la bibliothèque d'un cours (`--course [NOM]`), soit dans un créneau (`--day`/`--date` + `--time`),
- `.` : lance l'application graphique elle-même, comme `code .` — retourne la main immédiatement, sans bloquer le terminal.

La recherche de cours/fichier par nom est exacte (insensible à la casse). Cela permet à l'utilisateur d'avoir, en une seule commande, tous les documents de son heure de cours ouverts et prêts à l'emploi — un gain de temps immédiat entre deux cours ou juste avant d'entrer en classe.

Techniquement, cette commande fonctionne **sans lancer l'interface graphique** (sauf avec `.`) : [Lecture](core/src/main/java/com/courseflow/cli/Lecture.java) est un point d'entrée headless qui lit les mêmes données que l'application JavaFX ([EmploiDuTemps](core/src/main/java/com/courseflow/model/EmploiDuTemps.java) via [DataStore](core/src/main/java/com/courseflow/persistence/DataStore.java)) et réutilise la même logique d'ouverture de fichiers ([OuvreurFichiers](core/src/main/java/com/courseflow/io/OuvreurFichiers.java)). Voir [Installation et démarrage](#installation-et-démarrage) pour l'utiliser.

## Stack technique

- **Langage** : Java 21
- **UI** : JavaFX 21 (javafx-controls, javafx-fxml)
- **Build** : Maven multi-module — [core](core/) (`com.courseflow.core` : `cli`/`model`/`persistence`/`io`/`util`, zéro dépendance JavaFX/AtlantaFX, utilisé par la commande `lecture`) et [app](app/) (`com.courseflow.app` : `app`/`ui`, interface JavaFX, dépend de `core`). Séparation faite pour que `lecture` n'ait jamais à résoudre le module graphique au démarrage (javafx-maven-plugin configuré dans `app/pom.xml`).
- **Persistance** : à trancher (voir Phase 1 de la roadmap) — probablement fichier local structuré (JSON) dans un premier temps, avec migration possible vers SQLite si le volume de données ou les besoins de requêtage le justifient.
- **Ouverture de fichiers** : commande native du système (`xdg-open` sur Linux, `open` sur macOS, `start` sur Windows) via `ProcessBuilder`. `java.awt.Desktop` a été écarté : l'initialiser dans une appli JavaFX sur Linux charge un toolkit GTK concurrent de celui de JavaFX et fait planter la JVM.
- **Packaging final envisagé** : `jpackage` pour produire des installeurs natifs (Windows/macOS/Linux) et exposer la commande `lecture` comme exécutable accessible depuis le terminal.

## Installation et démarrage

### Prérequis
- JDK 21
- Maven 3.8+

### Lancer l'application
```
mvn -pl core -am install -DskipTests
mvn -f app/pom.xml javafx:run
```
(Projet Maven multi-module — voir [Stack technique](#stack-technique). `-f app/pom.xml` est
nécessaire : `mvn -pl app javafx:run` échoue avec « No plugin found for prefix 'javafx' », la
résolution du préfixe de plugin regarde le `pom.xml` du répertoire courant — le parent, qui ne
déclare pas `javafx-maven-plugin` — pas celui du module ciblé par `-pl`. La première commande
installe `core` dans le dépôt local Maven, nécessaire car `app` en dépend comme d'un artefact
normal une fois en dehors d'un build de reactor multi-module ; à refaire après une modification de
`core`.)

### Lancer les tests
```
mvn test
```

### Données utilisateur
Les données (cours, fichiers, créneaux) sont chargées et sauvegardées automatiquement dans `~/.courseflow/data.json` — à chaque modification et à la fermeture de la fenêtre. Aucune configuration manuelle n'est nécessaire : ce fichier est créé au premier lancement.

### Installer la commande `lecture`
```
ln -s "$(pwd)/bin/lecture" ~/.local/bin/lecture
```
(`~/.local/bin` doit être dans le `PATH`.) Ensuite, depuis n'importe où :
```
lecture                                    # ouvre les fichiers du créneau courant (jour/heure système)
lecture --day mardi --time 09:30           # cible un autre créneau
lecture --next                             # ouvre le créneau suivant
lecture --previous                         # ouvre le créneau précédent

lecture slot                               # affiche les infos du créneau courant, sans rien ouvrir
lecture slots --day mardi                  # liste les créneaux du Mardi
lecture schedule                           # affiche l'emploi du temps de la semaine en grille ASCII

lecture courses                            # liste tous les cours
lecture courses --missing-info             # cours sans aucun créneau planifié
lecture course "6e A - Mathématiques"      # détail d'un cours

lecture open-file --course "6e A - Mathématiques" --file exercices.pdf
lecture .                                  # lance l'application graphique (comme "code .")
lecture --help                             # liste toutes les commandes et options
```
(`bin/lecture` ci-dessus est l'outil de développement : il tourne depuis le checkout source via
Maven. Pour une installation native, sans dépendance à Maven ni au checkout, voir la section
suivante.)

### Installation packagée (jpackage, Linux)
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
`com.courseflow.core` — qui ne dépend jamais de JavaFX/AtlantaFX, voir [Stack
technique](#stack-technique) — écrit dans `target/dist/courseflow/bin/lecture` par
[bin/build-installer](bin/build-installer) pour l'app-image, et dans
`/opt/courseflow/bin/lecture-fast` par le `postinst` du `.deb` (voir plus bas). Résultat : ~0,6 s
par appel au lieu de ~1,8-2 s. Installer le paquet :
```
sudo dpkg -i target/dist/courseflow_1.0.0_amd64.deb
```
Le paquet `.deb` relie automatiquement la commande dans `/usr/local/bin/lecture` (déjà dans le
`PATH` par défaut sur Debian/Ubuntu) via un script `postinst` personnalisé
([packaging/deb/postinst](packaging/deb/postinst), retiré symétriquement par
[packaging/deb/postrm](packaging/deb/postrm) à la désinstallation) — `lecture` est donc
utilisable immédiatement après `dpkg -i`, sans étape manuelle. Pour l'app-image portable
(`target/dist/courseflow/`, sans passer par `dpkg`), relier la commande manuellement (même
principe que l'installation en mode dev) :
```
ln -s "$(pwd)/target/dist/courseflow/bin/lecture" ~/.local/bin/lecture
```
Nécessite un JDK complet (pas juste un JRE) pour `jpackage`/`jlink` ; `.deb` uniquement pour
l'instant (pas de `.rpm`, pas de build Windows/macOS — voir la roadmap).

### Générer des données de test
Pour explorer l'interface sans tout créer à la main :
```
bin/donnees-test
```
Génère 7 cours réalistes (niveau + matière, couleur aléatoire, 2-4 fichiers chacun) et 18 créneaux répartis sur la semaine, dans `~/.courseflow/data.json`. Les fichiers factices sont créés dans [test/](test/) (texte brut). Les données existantes sont sauvegardées en `.bak` avant d'être remplacées.
Le script lance `java` directement sur le seul module `core` (module-path + classpath mis en
cache dans `core/target/lecture-classpath.txt` lors du premier appel, régénéré si `pom.xml`
change) plutôt que de repasser par `mvn javafx:run` à chaque fois — ~0,3 s par appel au lieu de
~1,4 s, Maven n'ayant plus à se réinitialiser. Seul `lecture .` (lancement de l'appli graphique)
passe encore par `mvn javafx:run` en dev, sans impact puisqu'il n'est pas sur le chemin critique
d'usage répété. Pas besoin de builder un jar séparément pour l'instant (voir Phase 8 pour un vrai
exécutable packagé).

### Icône

L'icône source est vectorielle : [packaging/icon/courseflow.svg](packaging/icon/courseflow.svg)
(dossier + horloge, indigo `#4338CA` / ambre `#F59E0B`). Après toute modification, régénérer les
PNG dérivés :
```
bin/generer-icone
```
Le script rasterise le SVG (via ImageMagick `convert`) en `packaging/icon/courseflow.png` (512×512,
utilisé par `jpackage --icon` pour le lanceur natif) et en
`app/src/main/resources/com/courseflow/app/icon.png` (icône de fenêtre / barre des tâches, chargée
dans [App.java](app/src/main/java/com/courseflow/app/App.java)). Les deux PNG sont versionnés pour
que le build n'ait pas besoin d'ImageMagick.

**Charte graphique** : l'accent de l'interface reprend l'indigo de l'icône. Les fichiers
[charte-claire.css](app/src/main/resources/com/courseflow/app/charte-claire.css) et
[charte-sombre.css](app/src/main/resources/com/courseflow/app/charte-sombre.css) surchargent la
rampe `-color-accent-*` d'AtlantaFX (thème Primer) et sont chargés en plus du thème par
`App.appliquerTheme()`. Le logo est aussi affiché à côté du titre dans la barre latérale, et la
couleur par défaut d'un nouveau cours est cet indigo.

## État actuel du projet

- **Socle (modèle + persistance)** ✅ — [Fichier](core/src/main/java/com/courseflow/model/Fichier.java), [Cours](core/src/main/java/com/courseflow/model/Cours.java), [Creneau](core/src/main/java/com/courseflow/model/Creneau.java), [EmploiDuTemps](core/src/main/java/com/courseflow/model/EmploiDuTemps.java) ; persistance JSON via [DataStore](core/src/main/java/com/courseflow/persistence/DataStore.java) (Jackson) dans `~/.courseflow/data.json` ; couvert par des tests unitaires (`mvn test`).
- **Gestion des Cours (UI)** ✅ — [CoursGestionPane](app/src/main/java/com/courseflow/ui/CoursGestionPane.java) : créer/renommer/colorer un cours, lui attacher des fichiers (sélection individuelle, dossier réel référencé et actualisable avec sélection des fichiers à importer, ou lien web) ou en retirer plusieurs à la fois, supprimer un cours (icône corbeille). Recherche/filtre par nom ou tag ; étiquettes choisies dans un vocabulaire prédéfini/extensible ; un cours désigné "par défaut" dont les autres cours peuvent réutiliser des fichiers sans les dupliquer. Branché dans [App.java](app/src/main/java/com/courseflow/app/App.java) qui charge/sauvegarde automatiquement les données à chaque modification et à la fermeture. Testé manuellement dans l'application.
- **Emploi du temps (UI)** ✅ — [EmploiDuTempsPane](app/src/main/java/com/courseflow/ui/EmploiDuTempsPane.java) : grille des 7 jours (7h-20h), créer/modifier/supprimer un créneau via une boîte de dialogue (y compris salle et description, tous deux optionnels), rendu coloré par cours affichant nom + salle + horaires + description directement sur le bloc. Un créneau placé peut être glissé à la souris (déplacement libre entre jours et horaires, par pas de 10 min) et redimensionné en tirant son bord haut/bas. La largeur des colonnes s'adapte à la largeur de la fenêtre. Accessible via un onglet dédié dans [App.java](app/src/main/java/com/courseflow/app/App.java). Testé manuellement dans l'application.
- **Sélection de fichiers par créneau (UI)** ✅ — dans la boîte de dialogue d'un créneau : liste à cocher des fichiers du Cours associé (tout coché par défaut à la création), boutons "Tout cocher"/"Tout décocher", et bouton "Ouvrir maintenant" qui ouvre les fichiers cochés sans passer par le terminal. Testé manuellement dans l'application.
- **Commande `lecture` (CLI)** ✅ — [Lecture](core/src/main/java/com/courseflow/cli/Lecture.java) + [ArgumentsLecture](core/src/main/java/com/courseflow/cli/ArgumentsLecture.java) + [NavigationCreneaux](core/src/main/java/com/courseflow/cli/NavigationCreneaux.java) + [GrilleAscii](core/src/main/java/com/courseflow/cli/GrilleAscii.java) (testés unitairement) : sous-commandes `slot`/`slots`/`schedule`/`courses`/`course`/`open-file`, ciblage par `--day`/`--date`+`--time` ou `--next`/`--previous` (avertissement si le créneau résolu tombe un autre jour que la référence), et lancement de l'appli graphique (`lecture .`) — ouvre les fichiers via la logique partagée [OuvreurFichiers](core/src/main/java/com/courseflow/io/OuvreurFichiers.java). Vit dans le module [core](core/) (zéro dépendance JavaFX/AtlantaFX, voir [Stack technique](#stack-technique)) ; appel `java` direct (classpath mis en cache) plutôt que `mvn` à chaque exécution en dev, et lanceur natif jpackage contourné une fois packagé (voir Phase 8). Installable via [bin/lecture](bin/lecture). Testé manuellement en ligne de commande.
- **Accueil (UI)** ✅ — [AccueilPane](app/src/main/java/com/courseflow/ui/AccueilPane.java) : équivalent graphique de la commande `lecture`, affiche le créneau du moment (cours, salle, description, fichiers) avec un bouton pour tout ouvrir, et des boutons précédent/suivant (réutilise [NavigationCreneaux](core/src/main/java/com/courseflow/cli/NavigationCreneaux.java)) pour naviguer même pendant un creux entre deux cours. Onglet par défaut au lancement.
- **Thème (UI)** ✅ — [AtlantaFX](https://github.com/mkpaz/atlantafx) (thème Primer, licence MIT) appliqué au démarrage dans [App.java](app/src/main/java/com/courseflow/app/App.java) ; case "Thème sombre" en bas de la barre latérale, préférence persistée dans [Parametres](core/src/main/java/com/courseflow/model/Parametres.java). Les couleurs de chrome (grilles, séparateurs, textes secondaires) utilisent les jetons du thème (`-color-*`) pour rester correctes dans les deux modes ; les couleurs par cours restent codées en dur (choisies par l'utilisateur, hors thème).
- **Navigation** : barre latérale verticale (Accueil / Cours / Emploi du temps / Paramètres), fenêtre maximisée au lancement.
- **Reste à construire** : confort/robustesse, fonctionnalités avancées et packaging natif (Phases 6-8, optionnelles).

## Roadmap

### Phase 0 — Cadrage ✅
- [x] Définir le concept et le modèle de données (ce document)
- [x] Choisir le mécanisme de persistance → JSON local (voir Phase 1)
- [x] Décider du format d'invocation de `lecture` → script bash dans `~/.local/bin` appelant le jar (voir Phase 5)
- [ ] Esquisser les écrans principaux (wireframes rapides) — fait au fil de l'implémentation plutôt qu'en amont

### Phase 1 — Modèle de données & persistance ✅
- [x] Implémentation des classes `Cours`, `Fichier`, `Créneau`, `EmploiDuTemps`
- [x] Sérialisation/désérialisation JSON (Jackson) — sauvegarde et rechargement des données entre deux lancements
- [x] Emplacement de stockage des données utilisateur (`~/.courseflow/data.json`)
- [x] Tests unitaires de validation (modèle + persistance)

### Phase 2 — Gestion des Cours ✅
- [x] Écran de création/renommage/couleur d'un Cours
- [x] Ajout de fichiers (sélecteur natif, individuel ou multiple) et import d'un dossier entier (non récursif)
- [x] Suppression de fichiers (sélection multiple) et suppression d'un Cours (avec confirmation, cascade sur ses créneaux)
- [x] Liste/vue d'ensemble des Cours existants

### Phase 3 — Emploi du temps ✅
- [x] Grille des 7 jours (7h-20h en lignes)
- [x] Assignation d'un Cours à un créneau (création/édition/suppression via une boîte de dialogue au clic)
- [x] Vue visuelle claire (couleur du cours, nom, horaires), validée manuellement dans l'application
- [x] Déplacement d'un créneau à la souris (jour + heure, par pas de 10 min) et redimensionnement en tirant un bord
- [x] Largeur des colonnes adaptée à la largeur de la fenêtre (repli en défilement horizontal en dessous d'une largeur minimale)

### Phase 4 — Sélection de fichiers par créneau ✅
- [x] Pour un créneau donné, interface de sélection des fichiers à utiliser parmi ceux du Cours associé (liste à cocher, tout coché par défaut)
- [x] Distinction visuelle entre "fichiers du cours" (liste complète affichée) et "fichiers sélectionnés pour cette séance" (cases cochées)
- [x] Bouton "ouvrir maintenant" depuis l'interface graphique (ouvre les fichiers cochés du créneau, sans passer par le terminal)

### Phase 5 — Commande `lecture` ✅
- [x] Mode d'exécution headless / point d'entrée CLI dédié (`com.courseflow.cli.Lecture`)
- [x] Détection automatique du créneau courant (jour + heure système)
- [x] Ouverture des fichiers associés via une commande native (`xdg-open`/`open`/`start`, voir Stack technique)
- [x] Gestion des cas limites : aucun créneau à l'heure actuelle (message clair), fichier non ouvrable (signalé sans bloquer les autres), plusieurs fichiers ouverts en une fois
- [x] Option pour cibler un autre créneau que celui du moment (`--day`/`--date`, `--time`)
- [x] `slots` liste les créneaux d'un jour au lieu d'en ouvrir un
- [x] `--next`/`--previous` ouvrent le créneau précédent/suivant de la semaine (navigation circulaire), avec avertissement si le jour résolu diffère du jour demandé
- [x] `slot` affiche les informations du créneau résolu (cours, salle, description, fichiers) au lieu de les ouvrir
- [x] `schedule` affiche l'emploi du temps de la semaine en tableau ASCII (une colonne par jour, jour courant marqué), créneaux rendus en boîtes empilées par ordre chronologique (nom/salle/description toujours affichés, fichiers selon la durée)
- [x] `courses`/`course`/`open-file` : lister les cours (avec filtre `--missing-info`), afficher le détail d'un cours, ouvrir un fichier précis par nom (dans un cours ou un créneau)
- [x] `lecture .` lance l'application graphique (comme `code .`), détachée du terminal
- [x] Script d'installation ([bin/lecture](bin/lecture)) appelant le point d'entrée CLI via `mvn javafx:run -Djavafx.mainClass=...`

### Phase 6 — Confort & robustesse (quasi terminée — 2 items reportés)
- [x] Gestion des erreurs (fichier manquant, chemin invalide) avec message clair à l'utilisateur — [OuvreurFichiers](core/src/main/java/com/courseflow/io/OuvreurFichiers.java) détecte un chemin local manquant avant de tenter l'ouverture (la commande native comme `xdg-open` échoue silencieusement de notre point de vue sinon) et renvoie un message par fichier (libellé + raison)
- [x] Annuler/rétablir (Ctrl+Z / Ctrl+Maj+Z) sur l'emploi du temps, pour rattraper un créneau déplacé, redimensionné, supprimé, dupliqué ou modifié par erreur — pile d'instantanés dans [EmploiDuTempsPane](app/src/main/java/com/courseflow/ui/EmploiDuTempsPane.java), scopée à cet onglet (n'affecte pas les cours)
- [x] Édition rapide — glisser-déposer des fichiers depuis l'explorateur du système sur la liste d'un cours ; dupliquer un créneau (bouton dans sa boîte de dialogue, la copie se replace ensuite à la souris)
- [ ] Édition rapide — réorganisation des fichiers dans la liste d'un cours, duplication d'un jour entier vers un autre (mis de côté pour l'instant)
- [x] Recherche/filtre dans la liste des cours et fichiers — [CoursGestionPane](app/src/main/java/com/courseflow/ui/CoursGestionPane.java) : champ de recherche par nom (cours et fichiers), la recherche fichiers matche aussi les tags
- [x] Étiquettes/tags sur les fichiers, choisies dans un vocabulaire prédéfini et extensible (`dm`, `td`, `correction`, `cm` par défaut) pour trier indépendamment du cours
- [ ] Aperçu rapide d'un fichier (PDF/image) directement dans l'appli, sans ouvrir le logiciel externe (mis de côté pour l'instant — aurait nécessité une nouvelle dépendance pour le rendu PDF)
- [x] Sauvegarde/export de la configuration (pour changer de machine ou sauvegarder) — section "Sauvegarde" dans [ParametresPane](app/src/main/java/com/courseflow/ui/ParametresPane.java)
- [x] Dossier réel du disque référencé par un Cours (persistant, pas un import ponctuel), avec sélection explicite des fichiers à importer (récursif optionnel) et actualisation pour importer les nouveaux fichiers sans dupliquer l'existant
- [x] Cours par défaut désigné parmi les cours existants : les autres cours peuvent déplier ses fichiers et cocher lesquels réutiliser, sans dupliquer le fichier (remplace l'idée abandonnée de lier n'importe quel cours à n'importe quel cours, qui posait un problème de cycles) — ni les dossiers virtuels (abandonnés au profit des tags)
- [x] Ajout de liens web (URL) comme "fichiers" ouvrables au même titre que les fichiers locaux
- [x] Suppression d'un Cours via la même icône corbeille que la suppression de fichier (cohérence visuelle)
- [x] Refonte graphique professionnelle, sobre mais réactive : thème(s), animations/transitions, micro-interactions — remplace le rendu par défaut JavaFX utilisé jusqu'ici (le style avait été volontairement mis de côté jusqu'à cette phase) — [AtlantaFX](https://github.com/mkpaz/atlantafx) (thème Primer), clair/sombre avec bascule persistée dans les paramètres ; couleurs de chrome codées en dur remplacées par les jetons du thème pour un rendu correct dans les deux ; micro-interactions (hover/focus/pressed) fournies par le thème. Pas d'animations/transitions custom écrites à la main pour l'instant.
- [x] Section "Accueil" dans la barre latérale : équivalent graphique de la commande `lecture`, propose directement les fichiers du créneau de l'heure avec boutons précédent/suivant (pour enchaîner sans effet de battement entre deux créneaux) — [AccueilPane](app/src/main/java/com/courseflow/ui/AccueilPane.java), onglet par défaut au lancement

### Phase 7 — Fonctionnalités avancées (optionnel, post-MVP)
- [x] Gestion de semaines alternées (semaine A / semaine B) : date d'ancrage réglable dans les Paramètres (parité par rapport à cette date), créneaux réglables sur "toutes les semaines" (défaut, comportement historique inchangé), "semaine A" ou "semaine B" ; sélecteur de semaine affichée dans l'emploi du temps, filtrage automatique dans l'Accueil et le CLI (`lecture`, `slot`, `slots`, `schedule`, `open-file`). La navigation précédent/suivant ignore volontairement l'alternance pour l'instant (parcourt jour/heure comme avant)
- Cycle de N semaines nommées (au-delà de la simple alternance A/B) : généraliserait `TypeSemaine` (A/B/toutes) en une liste de semaines nommées avec parité mod N par rapport à la date d'ancrage, plutôt qu'un simple mod 2 — remplacerait le modèle actuel plutôt que de l'étendre, avec de la nouvelle UI pour nommer/gérer/réordonner les semaines (sélecteur, dialogue de créneau, `lecture week`, `GrilleAscii`)
- Notifications/rappels avant le début d'un créneau
- Historique ou statistiques d'usage
- Synchronisation multi-appareils

### Phase 8 — Packaging & distribution
- [x] Génération d'installeurs natifs via `jpackage` (Linux) : [bin/build-installer](bin/build-installer)
  construit une image d'exécution minimale via `jlink`, puis un app-image portable et un paquet
  `.deb` avec `jpackage`, embarquant deux lanceurs natifs partageant le même runtime —
  `courseflow` (GUI) et `lecture` (CLI, [packaging/lecture-launcher.properties](packaging/lecture-launcher.properties)).
  `lecture .` fonctionne aussi depuis le binaire packagé (démarre l'exécutable `courseflow`
  voisin, détaché via `setsid`, avec les variables d'environnement propres au lanceur `lecture`
  retirées pour ne pas perturber son démarrage — voir `lancerInterfaceGraphiqueVoisine` dans
  [Lecture.java](core/src/main/java/com/courseflow/cli/Lecture.java)). Pas de `.rpm` (`rpmbuild`
  absent de la machine de dev) ni de build Windows/macOS (nécessiterait des machines dédiées).
- [x] `lecture` sans le coût du lanceur natif jpackage : ce dernier interroge `rpm`/`dpkg` à
  chaque démarrage pour s'auto-identifier (~1 à 1,3 s perdues à chaque lancement, mesuré au
  `strace`). `bin/build-installer` le remplace, dans l'app-image, par un script qui appelle
  directement le runtime Java packagé sur le module `com.courseflow.core` (jamais de dépendance
  JavaFX/AtlantaFX à résoudre) ; le `postinst` du `.deb` fait de même en écrivant
  `/opt/courseflow/bin/lecture-fast`. `lancerInterfaceGraphiqueVoisine` (dans
  [Lecture.java](core/src/main/java/com/courseflow/cli/Lecture.java)) retrouve l'exécutable
  `courseflow` voisin via la propriété système `courseflow.bindir` positionnée par ces scripts
  (repli sur le chemin de l'exécutable courant si absente, pour le lanceur natif jpackage
  toujours présent mais non utilisé par défaut). Gain mesuré : ~1,8-2 s → ~0,6 s par appel.
- [x] Exposition de la commande `lecture` dans le PATH : automatique pour le paquet `.deb` via un
  script `postinst`/`postrm` `jpackage` personnalisé qui relie/retire `/usr/local/bin/lecture`
  ([packaging/deb/postinst](packaging/deb/postinst), [packaging/deb/postrm](packaging/deb/postrm),
  activés via `--resource-dir packaging/deb` dans [bin/build-installer](bin/build-installer)) ;
  pour l'app-image portable (sans `dpkg`), même principe qu'en mode dev mais vers le binaire
  packagé (`ln -s .../courseflow/bin/lecture ~/.local/bin/lecture`), documenté ci-dessus.
- [x] Documentation d'installation et de mise à jour — section "Installation packagée" ci-dessus
- [~] Icône de l'application — placeholder SVG en place ([packaging/icon/](packaging/icon/), câblé dans la fenêtre JavaFX et `jpackage --icon`), à remplacer par le design définitif (voir section "Icône")

## Prochaine étape suggérée

Le parcours principal (Phases 0-5) est complet : créer des cours, les remplir de fichiers, les placer dans l'emploi du temps, choisir les fichiers par séance, et les ouvrir en une commande. Les phases restantes (6-8) sont des améliorations optionnelles de confort, de fonctionnalités et de packaging — à prioriser selon l'usage réel de l'application plutôt qu'en amont.
