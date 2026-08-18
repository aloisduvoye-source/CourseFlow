# TeacherFlow

TeacherFlow est une application de bureau (JavaFX) qui organise les fichiers de cours d'un enseignant autour de son emploi du temps. L'objectif : ne plus jamais perdre de temps à chercher le bon PDF, le bon diaporama ou la bonne feuille d'exercices avant un cours — un seul clic (ou une commande) ouvre exactement les fichiers dont on a besoin, au bon moment.

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
 └─ sous-ensemble de Fichiers sélectionnés pour CE créneau
    (sélectionnés parmi les fichiers du Cours associé — pas une nouvelle liste libre)

EmploiDuTemps
 └─ ensemble des Créneaux de la semaine (grille type "semaine type", récurrente)
```

Point important de la structure : les fichiers appartiennent au **Cours**, pas au créneau. Le créneau ne fait que *piocher* dans les fichiers du cours. Cela évite de dupliquer les mêmes fichiers d'une semaine à l'autre : on les attache une fois au Cours, et on affine juste la sélection semaine par semaine si besoin.

## La commande `lecture`

Lancée dans un terminal, `lecture` :
- sans argument : ouvre les fichiers du créneau correspondant au jour/heure actuels,
- `--jour <nom>` et/ou `--heure HH:mm` : cible un autre créneau (ex. préparer la séance suivante) — `--jour` seul, **sans** `--heure`, liste plutôt tous les créneaux de ce jour (utile pour voir le programme de la journée sans rien ouvrir),
- `-n` / `-p` : ouvre le créneau **suivant**/**précédent** dans la semaine plutôt que celui du moment (boucle : après le dernier créneau du Dimanche, `-n` revient au premier du Lundi, et inversement pour `-p`) — si ce créneau tombe un autre jour que celui demandé, un message le signale avant d'ouvrir les fichiers,
- `-l` : liste les fichiers du créneau résolu (courant, ciblé, ou via `-n`/`-p`) au lieu de les ouvrir,
- `.` : lance l'application graphique elle-même, comme `code .` — retourne la main immédiatement, sans bloquer le terminal.

Cela permet à l'utilisateur d'avoir, en une seule commande, tous les documents de son heure de cours ouverts et prêts à l'emploi — un gain de temps immédiat entre deux cours ou juste avant d'entrer en classe.

Techniquement, cette commande fonctionne **sans lancer l'interface graphique** (sauf avec `.`) : [Lecture](src/main/java/com/teacherflow/cli/Lecture.java) est un point d'entrée headless qui lit les mêmes données que l'application JavaFX ([EmploiDuTemps](src/main/java/com/teacherflow/model/EmploiDuTemps.java) via [DataStore](src/main/java/com/teacherflow/persistence/DataStore.java)) et réutilise la même logique d'ouverture de fichiers ([OuvreurFichiers](src/main/java/com/teacherflow/io/OuvreurFichiers.java)). Voir [Installation et démarrage](#installation-et-démarrage) pour l'utiliser.

## Stack technique

- **Langage** : Java 21
- **UI** : JavaFX 21 (javafx-controls, javafx-fxml)
- **Build** : Maven (javafx-maven-plugin déjà configuré)
- **Persistance** : à trancher (voir Phase 1 de la roadmap) — probablement fichier local structuré (JSON) dans un premier temps, avec migration possible vers SQLite si le volume de données ou les besoins de requêtage le justifient.
- **Ouverture de fichiers** : commande native du système (`xdg-open` sur Linux, `open` sur macOS, `start` sur Windows) via `ProcessBuilder`. `java.awt.Desktop` a été écarté : l'initialiser dans une appli JavaFX sur Linux charge un toolkit GTK concurrent de celui de JavaFX et fait planter la JVM.
- **Packaging final envisagé** : `jpackage` pour produire des installeurs natifs (Windows/macOS/Linux) et exposer la commande `lecture` comme exécutable accessible depuis le terminal.

## Installation et démarrage

### Prérequis
- JDK 21
- Maven 3.8+

### Lancer l'application
```
mvn javafx:run
```

### Lancer les tests
```
mvn test
```

### Données utilisateur
Les données (cours, fichiers, créneaux) sont chargées et sauvegardées automatiquement dans `~/.teacherflow/data.json` — à chaque modification et à la fermeture de la fenêtre. Aucune configuration manuelle n'est nécessaire : ce fichier est créé au premier lancement.

### Installer la commande `lecture`
```
ln -s "$(pwd)/bin/lecture" ~/.local/bin/lecture
```
(`~/.local/bin` doit être dans le `PATH`.) Ensuite, depuis n'importe où :
```
lecture                              # ouvre les fichiers du créneau courant (jour/heure système)
lecture --jour Mardi --heure 09:30   # cible un autre créneau
lecture --jour Mardi                 # liste les créneaux du Mardi, sans rien ouvrir
lecture -n                           # ouvre le créneau suivant
lecture -p                           # ouvre le créneau précédent
lecture -l                           # liste les fichiers du créneau courant, sans les ouvrir
lecture -n -l                        # liste les fichiers du créneau suivant
lecture .                            # lance l'application graphique (comme "code .")
```
Le script appelle `mvn javafx:run` avec un point d'entrée alternatif ([Lecture](src/main/java/com/teacherflow/cli/Lecture.java)) — pas besoin de builder un jar séparément pour l'instant (voir Phase 8 pour un vrai exécutable packagé).

## État actuel du projet

- **Socle (modèle + persistance)** ✅ — [Fichier](src/main/java/com/teacherflow/model/Fichier.java), [Cours](src/main/java/com/teacherflow/model/Cours.java), [Creneau](src/main/java/com/teacherflow/model/Creneau.java), [EmploiDuTemps](src/main/java/com/teacherflow/model/EmploiDuTemps.java) ; persistance JSON via [DataStore](src/main/java/com/teacherflow/persistence/DataStore.java) (Jackson) dans `~/.teacherflow/data.json` ; couvert par des tests unitaires (`mvn test`).
- **Gestion des Cours (UI)** ✅ — [CoursGestionPane](src/main/java/com/teacherflow/ui/CoursGestionPane.java) : créer/renommer/colorer un cours, lui attacher des fichiers (sélection individuelle ou import d'un dossier entier) ou en retirer plusieurs à la fois, supprimer un cours. Branché dans [App.java](src/main/java/com/teacherflow/app/App.java) qui charge/sauvegarde automatiquement les données à chaque modification et à la fermeture. Testé manuellement dans l'application.
- **Emploi du temps (UI)** ✅ — [EmploiDuTempsPane](src/main/java/com/teacherflow/ui/EmploiDuTempsPane.java) : grille des 7 jours (7h-20h), créer/modifier/supprimer un créneau via une boîte de dialogue, rendu coloré par cours. Un créneau placé peut être glissé à la souris (déplacement libre entre jours et horaires, par pas de 10 min) et redimensionné en tirant son bord haut/bas. La largeur des colonnes s'adapte à la largeur de la fenêtre. Accessible via un onglet dédié dans [App.java](src/main/java/com/teacherflow/app/App.java). Testé manuellement dans l'application.
- **Sélection de fichiers par créneau (UI)** ✅ — dans la boîte de dialogue d'un créneau : liste à cocher des fichiers du Cours associé (tout coché par défaut à la création), boutons "Tout cocher"/"Tout décocher", et bouton "Ouvrir maintenant" qui ouvre les fichiers cochés sans passer par le terminal. Testé manuellement dans l'application.
- **Commande `lecture` (CLI)** ✅ — [Lecture](src/main/java/com/teacherflow/cli/Lecture.java) + [ArgumentsLecture](src/main/java/com/teacherflow/cli/ArgumentsLecture.java) + [NavigationCreneaux](src/main/java/com/teacherflow/cli/NavigationCreneaux.java) (testés unitairement) : créneau courant, liste du jour (`--jour` seul), créneau précédent/suivant (`-p`/`-n`, avec avertissement si le créneau résolu tombe un autre jour que celui demandé), liste des fichiers sans les ouvrir (`-l`), et lancement de l'appli graphique (`lecture .`) — ouvre les fichiers via la logique partagée [OuvreurFichiers](src/main/java/com/teacherflow/io/OuvreurFichiers.java). Installable via [bin/lecture](bin/lecture). Testé manuellement en ligne de commande.
- **Design UI** : palette sobre et professionnelle ([teacherflow.css](src/main/resources/css/teacherflow.css)), navigation verticale, boîtes de dialogue pour l'édition des créneaux.
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
- [x] Emplacement de stockage des données utilisateur (`~/.teacherflow/data.json`)
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
- [x] Mode d'exécution headless / point d'entrée CLI dédié (`com.teacherflow.cli.Lecture`)
- [x] Détection automatique du créneau courant (jour + heure système)
- [x] Ouverture des fichiers associés via une commande native (`xdg-open`/`open`/`start`, voir Stack technique)
- [x] Gestion des cas limites : aucun créneau à l'heure actuelle (message clair), fichier non ouvrable (signalé sans bloquer les autres), plusieurs fichiers ouverts en une fois
- [x] Option pour cibler un autre créneau que celui du moment (`--jour`, `--heure`)
- [x] `--jour` seul liste les créneaux du jour au lieu d'en ouvrir un
- [x] `-p`/`-n` ouvrent le créneau précédent/suivant de la semaine (navigation circulaire), avec avertissement si le jour résolu diffère du jour demandé
- [x] `-l` liste les fichiers du créneau résolu au lieu de les ouvrir
- [x] `lecture .` lance l'application graphique (comme `code .`), détachée du terminal
- [x] Script d'installation ([bin/lecture](bin/lecture)) appelant le point d'entrée CLI via `mvn javafx:run -Djavafx.mainClass=...`

### Phase 6 — Confort & robustesse
- Gestion des erreurs (fichier manquant, chemin invalide) avec message clair à l'utilisateur
- Édition rapide (glisser-déposer de fichiers, réorganisation, duplication de créneau/semaine)
- Recherche/filtre dans la liste des cours et fichiers
- Sauvegarde/export de la configuration (pour changer de machine ou sauvegarder)

### Phase 7 — Fonctionnalités avancées (optionnel, post-MVP)
- Gestion de semaines alternées (semaine A / semaine B)
- Notifications/rappels avant le début d'un créneau
- Historique ou statistiques d'usage
- Synchronisation multi-appareils

### Phase 8 — Packaging & distribution
- Génération d'installeurs natifs via `jpackage`
- Exposition de la commande `lecture` dans le PATH utilisateur
- Documentation d'installation et de mise à jour

## Prochaine étape suggérée

Le parcours principal (Phases 0-5) est complet : créer des cours, les remplir de fichiers, les placer dans l'emploi du temps, choisir les fichiers par séance, et les ouvrir en une commande. Les phases restantes (6-8) sont des améliorations optionnelles de confort, de fonctionnalités et de packaging — à prioriser selon l'usage réel de l'application plutôt qu'en amont.
