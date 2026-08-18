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
- détermine le créneau concerné (par défaut : le créneau correspondant au jour/heure actuels — avec la possibilité de préciser un jour/heure/cours en argument pour préparer une séance à l'avance),
- récupère la liste des fichiers sélectionnés pour ce créneau,
- ouvre chacun de ces fichiers avec l'application par défaut du système d'exploitation.

Cela permet à l'utilisateur d'avoir, en une seule commande, tous les documents de son heure de cours ouverts et prêts à l'emploi — un gain de temps immédiat entre deux cours ou juste avant d'entrer en classe.

Techniquement, cette commande doit pouvoir fonctionner **sans lancer l'interface graphique complète** : il s'agit donc d'un mode d'exécution "headless" de l'application (ou d'un petit exécutable compagnon), qui lit les mêmes données que l'application JavaFX.

## Stack technique

- **Langage** : Java 21
- **UI** : JavaFX 21 (javafx-controls, javafx-fxml)
- **Build** : Maven (javafx-maven-plugin déjà configuré)
- **Persistance** : à trancher (voir Phase 1 de la roadmap) — probablement fichier local structuré (JSON) dans un premier temps, avec migration possible vers SQLite si le volume de données ou les besoins de requêtage le justifient.
- **Ouverture de fichiers** : `java.awt.Desktop` (API standard, multiplateforme, ouvre un fichier avec l'application associée du système).
- **Packaging final envisagé** : `jpackage` pour produire des installeurs natifs (Windows/macOS/Linux) et exposer la commande `lecture` comme exécutable accessible depuis le terminal.

## État actuel du projet

- **Socle (modèle + persistance)** ✅ — [Fichier](src/main/java/com/teacherflow/model/Fichier.java), [Cours](src/main/java/com/teacherflow/model/Cours.java), [Creneau](src/main/java/com/teacherflow/model/Creneau.java), [EmploiDuTemps](src/main/java/com/teacherflow/model/EmploiDuTemps.java) ; persistance JSON via [DataStore](src/main/java/com/teacherflow/persistence/DataStore.java) (Jackson) dans `~/.teacherflow/data.json` ; couvert par des tests unitaires (`mvn test`).
- **Gestion des Cours (UI)** ✅ — [CoursGestionPane](src/main/java/com/teacherflow/ui/CoursGestionPane.java) : créer/renommer/colorer un cours, lui attacher des fichiers (sélection individuelle ou import d'un dossier entier) ou en retirer plusieurs à la fois, supprimer un cours. Branché dans [App.java](src/main/java/com/teacherflow/app/App.java) qui charge/sauvegarde automatiquement les données à chaque modification et à la fermeture. Testé manuellement dans l'application.
- **Emploi du temps (UI)** ✅ — [EmploiDuTempsPane](src/main/java/com/teacherflow/ui/EmploiDuTempsPane.java) : grille hebdomadaire Lundi-Samedi (8h-19h, pas de 30 min), créer/modifier/supprimer un créneau via une boîte de dialogue, rendu coloré par cours. Accessible via un onglet dédié dans [App.java](src/main/java/com/teacherflow/app/App.java). Testé manuellement dans l'application.
- **Reste à construire** : la sélection de fichiers par créneau (Phase 4) et la commande `lecture` (Phase 5).

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
- [x] Grille hebdomadaire (Lundi-Samedi en colonnes, 8h-19h par pas de 30 min en lignes)
- [x] Assignation d'un Cours à un créneau (création/édition/suppression via une boîte de dialogue au clic)
- [x] Vue visuelle claire (couleur du cours, nom, horaires), validée manuellement dans l'application

### Phase 4 — Sélection de fichiers par créneau
- Pour un créneau donné, interface de sélection des fichiers à utiliser parmi ceux du Cours associé
- Distinction visuelle entre "fichiers du cours" et "fichiers sélectionnés pour cette séance"
- Bouton "ouvrir maintenant" depuis l'interface graphique (ouvre les fichiers du créneau sélectionné, sans passer par le terminal)

### Phase 5 — Commande `lecture`
- Mode d'exécution headless / point d'entrée CLI dédié
- Détection automatique du créneau courant (jour + heure système)
- Ouverture des fichiers associés via `Desktop.open`
- Gestion des cas limites : aucun créneau à l'heure actuelle, fichier introuvable/déplacé, plusieurs fichiers à ouvrir simultanément
- Option pour cibler un autre créneau que celui du moment (ex. préparer le cours suivant, ou un jour donné)

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

Phase 4 — dans l'écran d'un créneau (clic sur un bloc de la grille), permettre de choisir précisément quels fichiers du Cours associé sont utiles pour cette séance (au lieu d'utiliser toute la bibliothèque du cours par défaut), avec un bouton "ouvrir maintenant". C'est le dernier maillon manquant avant la commande `lecture` (Phase 5), qui n'a de sens que si des créneaux ont une sélection de fichiers définie.
