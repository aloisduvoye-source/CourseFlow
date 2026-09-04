# CourseFlow — Bilan complet & pistes d'évolution à long terme

> **Nature de ce document** : contrairement à la Roadmap du
> [Guide développeur](GUIDE_DEVELOPPEUR.md#état-davancement--roadmap), qui suit l'avancement réel
> du projet phase par phase, ce document est un inventaire volontairement
> large et spéculatif — un brainstorm structuré de tout ce qui pourrait faire évoluer CourseFlow,
> du correctif mineur à l'idée de rupture. Rien ici n'est engagé ni priorisé par défaut : c'est
> une réserve d'idées à piocher selon l'usage réel, pas un plan de développement. Écrit le
> 2026-09-04, à jour du commit `1ea0a7d` (131 commits).

## Sommaire

1. [Contexte et méthode](#1-contexte-et-méthode)
2. [État des lieux](#2-état-des-lieux)
3. [Pistes — Fonctionnalités métier](#3-pistes--fonctionnalités-métier)
4. [Pistes — UX et ergonomie](#4-pistes--ux-et-ergonomie)
5. [Pistes — Interface graphique et design visuel](#5-pistes--interface-graphique-et-design-visuel)
6. [Pistes — Accessibilité](#6-pistes--accessibilité)
7. [Pistes — CLI `lecture`](#7-pistes--cli-lecture)
8. [Pistes — Architecture, qualité technique, expérience développeur](#8-pistes--architecture-qualité-technique-expérience-développeur)
9. [Pistes — Données, sauvegarde et synchronisation](#9-pistes--données-sauvegarde-et-synchronisation)
10. [Pistes — Packaging et déploiement multiplateforme](#10-pistes--packaging-et-déploiement-multiplateforme)
11. [Pistes — Distribution, écosystème, communauté](#11-pistes--distribution-écosystème-communauté)
12. [Pistes — Sécurité et confidentialité](#12-pistes--sécurité-et-confidentialité)
13. [Vision long terme (horizon incertain)](#13-vision-long-terme-horizon-incertain)
14. [Comment prioriser tout ça](#14-comment-prioriser-tout-ça)

---

## 1. Contexte et méthode

CourseFlow résout un problème précis : un enseignant qui retrouve, avant chaque heure de cours,
les bons fichiers parmi les mêmes supports réutilisés semaine après semaine. Le projet a déjà
livré ce parcours de bout en bout (Phases 0-5 de la Roadmap) et une bonne partie du confort
attendu (Phase 6). Ce document part de cette base solide pour explorer, sans filtre de faisabilité
immédiate, où le projet *pourrait* aller — à un an, cinq ans, ou "si un jour ça devient plus qu'un
outil perso".

Chaque section "Pistes" liste des idées indépendantes, formulées comme **Titre** — description
(valeur apportée, contrainte notable s'il y en a une). Aucune numérotation de priorité n'est
assignée dans le corps du texte : la section 14 propose une méthode pour trier, pas un verdict.

---

## 2. État des lieux

### 2.1 Vision et proposition de valeur

Un enseignant définit des **Cours** réutilisables, chacun avec sa bibliothèque de fichiers ; il
les place dans un **emploi du temps** type ; pour chaque **créneau**, il affine quels fichiers de
la bibliothèque sont utiles *cette séance-là*. Le jour J, un clic (Accueil) ou une commande
(`lecture`) ouvre exactement ce qu'il faut. Le principe structurant — les fichiers appartiennent
au Cours, pas au créneau, qui ne fait que piocher dedans — évite toute duplication et reste
cohérent dans tout le code lu pour ce bilan.

### 2.2 Stack technique et architecture

| Aspect | État actuel |
|---|---|
| Langage | Java 21, modules JPMS explicites (`module-info.java` dans les deux modules) |
| UI | JavaFX 21.0.2 + [AtlantaFX](https://github.com/mkpaz/atlantafx) 2.1.0 (thème Primer, clair/sombre) |
| Build | Maven multi-module : `core` (`com.courseflow.core`, zéro dépendance graphique) + `app` (`com.courseflow.app`, dépend de `core`) |
| Persistance | JSON local (Jackson 2.17.2 + module `jsr310`) dans `~/.courseflow/data.json` |
| Tests | JUnit 5.10.3 — uniquement sur `core` |
| Packaging | `jpackage`/`jlink`, `.deb` Linux fonctionnel |

La séparation `core`/`app` est le choix architectural le plus structurant du projet : elle existe
uniquement pour que la commande `lecture` démarre en ~0,6 s sans jamais avoir à résoudre
JavaFX/AtlantaFX. C'est une contrainte de performance CLI qui a façonné toute l'organisation du
code (jusqu'au détail de `LanceurGraphiqueVoisin` qui relance l'exécutable graphique voisin en
sous-processus détaché plutôt que d'appeler l'UI in-process). Tout ajout futur doit respecter
cette frontière : rien de `javafx.*`/`atlantafx.*` ne doit fuiter dans `core`.

Taille actuelle du code (hors ressources) :

- `app/ui` : 3331 lignes (9 classes — panes + `Icons`/`IconesFichier`/`Couleurs`/`TagPills`/`Toast`)
- `app/app` (`App.java`) : 219 lignes
- `core/model` : 750 lignes (8 classes)
- `core/cli` : 1357 lignes (10 classes)
- `core/persistence` + `io` + `util` : 309 lignes
- `core` tests : 1037 lignes (8 fichiers)
- **Total** : ~7025 lignes Java, 131 commits

### 2.3 Modèle de données

```
Fichier          — chemin (ou URL), nom d'affichage optionnel, tags
Cours            — nom, couleur, fichiers possédés, dossiers référencés, fichiers "liés"
                    (IDs de fichiers empruntés à un autre cours, résolus en un seul saut)
Creneau          — jour, heure début/fin, coursId, salle/description optionnelles,
                    sous-ensemble de fichiers sélectionnés, TypeSemaine (TOUTES/A/B)
EmploiDuTemps    — liste de Cours + liste de Creneau (racine sérialisée)
Parametres       — jours affichés, pas de déplacement, plage horaire, blocs horaires,
                    vocabulaire de tags + couleurs, cours par défaut, thème, ancrage semaine A/B
DossierReference — chemin d'un dossier réel + fichiers déjà importés depuis lui
```

Le modèle est mutable, sans framework de state management : chaque pane reçoit un `Runnable
surChangement` déclenché après toute mutation, qui sauvegarde tout l'`EmploiDuTemps` sur disque.
Simple et efficace à cette échelle ; voir §2.10 pour ses limites.

### 2.4 Fonctionnalités actuelles — Interface graphique

| Écran | Rôle actuel |
|---|---|
| **Accueil** | Agenda du jour en cartes, navigation jour par jour, badge "en cours", ouverture en un clic, feedback de succès (toast) |
| **Cours** | Liste + recherche, création/renommage/couleur, bibliothèque de fichiers (ajout individuel, dossier référencé récursif, lien web), tags avec vocabulaire partagé, liaison de fichiers depuis n'importe quel autre cours |
| **Emploi du temps** | Grille 7 jours × plage configurable, créneaux glissables/redimensionnables à la souris **et au clavier** (flèches/Entrée/Suppr, ajouté cette session), undo/redo, semaines A/B, dialogue de créneau avec validation temps réel |
| **Paramètres** | Réglages de grille, blocs horaires (même interaction que la grille), semaines alternées, export/import JSON — regroupés en accordéon |

### 2.5 Fonctionnalités actuelles — CLI `lecture`

Point d'entrée headless complet et testé unitairement : `slot`, `slots`, `schedule` (grille ASCII
avec bordures Unicode), `courses`/`course`, `open-file`, `week` (lecture/réglage de l'alternance
A/B), navigation `--next`/`--previous` circulaire, ciblage par `--day`/`--date`+`--time`,
lancement de l'UI via `lecture .`. Architecture propre : `Lecture` ne fait qu'analyser les
arguments et aiguiller vers une classe dédiée par sous-commande (`CommandeOuverture`,
`CommandesConsultation`, `CommandeSemaine`), avec les helpers communs dans `OutilsLecture`.

### 2.6 Persistance, robustesse, sécurité actuelle

`DataStore` écrit de façon atomique (fichier `.tmp` + renommage) avec rotation de 3 sauvegardes
(`data.json.bak1..3`), met en quarantaine un fichier illisible plutôt que de l'écraser, et tolère
les champs JSON inconnus (compatibilité ascendante implicite — mais voir §2.10, aucune vraie
gestion de version de schéma). `OuvreurFichiers` ouvre via `ProcessBuilder` avec une liste
d'arguments (pas de concaténation shell), donc pas d'injection de commande triviale ; les chemins
locaux sont vérifiés avant ouverture. Aucune donnée n'est jamais transmise à un tiers : tout est
local.

### 2.7 Packaging et distribution actuels

`jpackage`/`jlink` produisent un app-image portable et un `.deb` Linux, avec deux lanceurs natifs
partageant un seul runtime (`courseflow` GUI, `lecture` CLI). Un soin particulier a été mis à
contourner la lenteur du lanceur natif jpackage pour `lecture` (~1,8-2 s → ~0,6 s), signe que la
réactivité de la CLI est un vrai critère de qualité pour ce projet. Pas de `.rpm`, pas de build
Windows/macOS, pas de mécanisme de mise à jour automatique.

### 2.8 Qualité, tests, expérience développeur

`core` est bien couvert (8 fichiers de test, 1037 lignes, argument-parsing/navigation/ASCII/
persistance/modèle). **`app` n'a aucun test automatisé** — seule validation manuelle documentée
dans le README pour chaque fonctionnalité UI. Pas de CI configurée dans le dépôt (pas de
`.github/workflows`). Pas d'analyse statique. Scripts de dev soignés (`bin/lecture`,
`bin/donnees-test`, `bin/generer-icone`, `bin/build-installer`), avec un vrai effort de perf sur
les boucles de développement répétées (cache de classpath, appel direct du runtime plutôt que
`mvn` à chaque fois).

### 2.9 Points forts

- Séparation `core`/`app` disciplinée et payante (perf CLI mesurée et documentée)
- Persistance très soignée pour un projet de cette taille (atomicité, backups, quarantaine)
- CLI complète, cohérente, bien testée — rare pour une appli qui a aussi une UI riche
- Modèle de données simple et sans duplication (fichiers possédés par le Cours, jamais copiés)
- Documentation (README) exceptionnellement à jour et précise, y compris sur le *pourquoi* des
  choix techniques (Desktop API écartée, lanceur natif contourné, etc.)
- Thème cohérent clair/sombre via des jetons plutôt que des couleurs codées en dur

### 2.10 Limites et dette technique connues

- **Zéro test automatisé sur `app`** (3331 + 219 lignes non couvertes) : toute régression UI
  n'est détectée que manuellement
- **Pas de CI** : rien n'empêche un commit cassant de passer inaperçu
- **Modèle mutable sans validation centralisée** : chaque pane revalide à sa façon (voir le
  travail de cette session sur la validation du nom de cours, désormais visuelle mais toujours
  locale à `CoursGestionPane`)
- **Pas de version de schéma explicite** sur `data.json` : la tolérance Jackson aux champs
  inconnus protège des ajouts, mais rien ne gère un renommage/une suppression de champ entre deux
  versions futures
- **Vocabulaire de tags plat et global** (une seule liste dans `Parametres`), pas de catégories/
  hiérarchie
- **Toute la chaîne d'ouverture de fichiers dépend de l'application par défaut du système** : pas
  de contrôle sur *quelle* application s'ouvre, pas d'aperçu intégré
- **Mono-utilisateur, mono-machine** par construction : `data.json` local, aucune notion de sync
  ou de compte
- **100 % français codé en dur** dans les chaînes UI/CLI (`NomsJours`, libellés de boutons,
  messages d'erreur) — aucune couche d'i18n
- **`EmploiDuTempsPane` et `CoursGestionPane` sont volumineux** (respectivement environ 1000 et
  900+ lignes) et mélangent construction d'UI, logique métier et gestion d'état ; pas encore de
  découpage en sous-composants réutilisables au-delà de `Icons`/`IconesFichier`/`TagPills`/`Toast`

---

## 3. Pistes — Fonctionnalités métier

- **Aperçu de fichier intégré (PDF/image)** — afficher un aperçu sans quitter l'appli ni ouvrir le
  logiciel externe (déjà noté "mis de côté" dans le README, faute de dépendance de rendu PDF
  choisie — [Apache PDFBox](https://pdfbox.apache.org/) ou un composant WebView chargeant le PDF
  via PDF.js seraient les deux options les plus réalistes).
- **Réorganisation manuelle des fichiers dans la liste d'un cours** — glisser-déposer pour
  réordonner (item déjà noté "mis de côté" en Phase 6), utile pour ranger par ordre d'usage plutôt
  que par ordre d'ajout.
- **Duplication d'un jour entier de l'emploi du temps** — copier tous les créneaux d'un jour vers
  un autre (item déjà noté "mis de côté").
- **Semaines cycliques au-delà de A/B** — généraliser `TypeSemaine` en semaines nommées avec
  parité mod N (déjà décrit dans la Roadmap Phase 7 comme un remplacement, pas une extension, du
  modèle actuel).
- **Notifications/rappels avant un créneau** — notification système quelques minutes avant le
  début d'un cours, avec bouton "Ouvrir maintenant" directement dans la notification.
- **Corbeille avec restauration différée** — remplacer/compléter le undo actuel (limité à la
  session, scope par écran) par une vraie corbeille consultable (cours/fichiers/créneaux/tags
  supprimés récemment), à la manière d'une corbeille système, avec purge après N jours.
- **Historique/statistiques d'usage** — "fichiers les plus ouverts", "cours jamais consultés
  depuis 3 mois", pour repérer les supports à mettre à jour ou les cours mal renseignés.
- **Notes libres attachées à un cours** (pas seulement à un créneau via `description`) — un
  espace "notes de préparation" par cours, pour des réflexions qui dépassent une séance précise.
- **Gestion des vacances scolaires (zones françaises A/B/C)** — import du calendrier officiel
  pour griser automatiquement les semaines sans cours plutôt que de gérer l'alternance A/B "à la
  main" pendant ces périodes.
- **Import depuis un ENT/Pronote/EDT** — si un export exploitable existe (iCal, CSV), importer
  l'emploi du temps réel de l'établissement au lieu de le ressaisir ; gain de temps considérable
  en septembre.
- **Export/impression de l'emploi du temps** — PDF ou image imprimable (poser sur le bureau, coller
  au mur), en plus de la grille ASCII déjà existante pour le terminal.
- **Export iCal** — pour que l'emploi du temps apparaisse dans Google Calendar/Outlook/Calendar.app
  sans resaisie, en lecture seule.
- **Recherche universelle ("spotlight")** — un raccourci global qui cherche à la fois dans les
  cours, les fichiers, les tags et les créneaux, plutôt que des recherches séparées par écran
  (actuellement `rechercheCours`/`rechercheFichiers` sont deux champs indépendants).
- **Tags hiérarchiques ou catégorisés** — regrouper `dm`/`td`/`correction`/`cm` sous des catégories
  ("type de document", "niveau de difficulté"...) une fois le vocabulaire plat actuel à l'étroit.
- **Association d'un fichier à plusieurs cours sans notion de "propriétaire"** — le modèle actuel
  (fichier possédé par un Cours, éventuellement lié par un autre) fonctionne bien pour le cas
  simple, mais un enseignant avec beaucoup de ressources transversales pourrait vouloir une vraie
  bibliothèque de fichiers indépendante des cours, associée à plusieurs par des tags/relations
  plutôt qu'une hiérarchie de possession. **Changement de modèle de données profond** — à ne
  considérer que si le besoin est confirmé par l'usage réel.
- **Mode "suppléant / remplaçant"** — un jeu de données allégé et partageable pour transmettre
  rapidement "ce qu'il faut ouvrir cette semaine" à un remplaçant sans lui donner accès à toute la
  bibliothèque.
- **Multi-établissements** — pour un enseignant intervenant sur plusieurs lieux, distinguer les
  créneaux par établissement (salle ne suffit pas toujours) avec filtrage dans l'Accueil/CLI.
- **Widget/tray icon système** — icône dans la barre des tâches/barre de menu affichant le
  prochain créneau sans ouvrir la fenêtre principale, avec un clic pour ouvrir ses fichiers.
- **Raccourci clavier global (system-wide)** — ouvrir les fichiers du créneau courant depuis
  n'importe quelle application, sans passer par le terminal ni faire apparaître la fenêtre.
- **QR code d'accès rapide** — généré pour un cours ou un créneau, à coller sur un support papier
  pour retrouver les fichiers numériques associés depuis un téléphone (nécessiterait une brique de
  consultation web ou mobile, voir §13).

---

## 4. Pistes — UX et ergonomie

- **Vue calendrier mensuelle** en complément de la grille hebdomadaire actuelle, pour visualiser
  les semaines A/B et les vacances sur un temps plus long.
- **Palette de commandes (Ctrl+K)** — recherche + actions rapides ("nouveau cours", "aller à
  mardi", "ouvrir les fichiers de...") sans naviguer à la souris dans les onglets, dans l'esprit
  des éditeurs de code modernes.
- **Glisser un fichier directement sur une case de la grille** pour créer un créneau *et*
  sélectionner ce fichier en un geste, au lieu de créer le créneau puis cocher le fichier dans le
  dialogue.
- **Mini-fenêtre flottante "toujours au-dessus"** montrant le prochain créneau, pour garder un œil
  dessus sans la fenêtre principale ouverte (proche de l'idée de tray icon, mais visible en
  permanence).
- **Onboarding première utilisation** — un tutoriel interactif bref au tout premier lancement
  (créer son premier cours, son premier créneau), plutôt que de découvrir seul via le README.
- **Aide contextuelle** — un point d'interrogation discret par écran renvoyant vers la section
  pertinente d'une documentation utilisateur (voir §11), en complément des tooltips déjà présents.
- **Réglage de la taille de police / zoom d'interface** — utile en vidéoprojection en classe, où
  l'enseignant pourrait vouloir agrandir temporairement l'Accueil pour le montrer aux élèves.
- **Mode "vidéoprojecteur"** — thème à fort contraste, gros caractères, dédié à un usage projeté
  plutôt qu'à l'écran personnel.
- **Statistiques d'usage douces** intégrées à l'Accueil ou aux Paramètres ("vos 5 fichiers les
  plus ouverts ce mois-ci"), qui demandent de tracer les ouvertures (actuellement non journalisées).
- **Vraie corbeille visible dans l'UI** (voir aussi §3) plutôt que des toasts "Annuler" limités
  dans le temps — un utilisateur qui revient le lendemain sur une suppression n'a aujourd'hui
  aucun recours.
- **Confirmation configurable** — un réglage global "toujours confirmer les suppressions" pour les
  utilisateurs qui préfèrent la sécurité à la fluidité (le compromis actuel, mix confirmation/undo
  choisi cette session, ne conviendra pas à tout le monde).
- **Historique de navigation façon "précédent" de navigateur** entre les écrans/cours consultés
  récemment, au-delà de la navigation jour par jour déjà présente dans l'Accueil.

---

## 5. Pistes — Interface graphique et design visuel

- **Design system formalisé** — actuellement, le style est appliqué par appels `setStyle(...)`
  dispersés dans chaque pane plutôt que par un jeu de classes CSS réutilisables ; un vrai fichier
  de composants (boutons, cartes, badges) réduirait la duplication visuelle et faciliterait les
  évolutions cohérentes.
- **Bibliothèque d'icônes plus riche** — le set actuel (`Icons.java`, tracés SVG maison, 9 icônes)
  couvre l'essentiel mais devra grandir avec les fonctionnalités ; évaluer une lib comme
  [Ikonli](https://kordamp.org/ikonli/) (intégration JavaFX native) plutôt que d'ajouter des
  tracés SVG à la main indéfiniment.
- **Thèmes de couleur additionnels** — au-delà de clair/sombre, proposer d'autres palettes
  AtlantaFX (Nord, Dracula, Solarized existent déjà comme thèmes AtlantaFX prêts à l'emploi) ou
  une couleur d'accent personnalisable par l'utilisateur (actuellement l'indigo de l'icône est
  codé en dur dans `charte-*.css`).
- **États vides illustrés** — les états vides actuels sont des `Label` texte simples ("Aucun cours
  ce jour-là", "Sélectionnez un cours...") ; une illustration légère renforcerait l'identité
  visuelle sans nuire à la sobriété recherchée.
- **Écran de démarrage (splash screen)** — utile une fois le chargement des données non
  instantané (gros volume de fichiers/cours), pour éviter une fenêtre vide le temps du chargement.
- **Accueil qui exploite mieux le grand écran** — actuellement une colonne centrée à largeur fixe
  (confirmé comme choix délibéré cette session) ; une évolution possible sans renoncer à la
  lisibilité serait d'ajouter une colonne secondaire sur très grand écran (ex. aperçu de la
  semaine à côté du détail du jour) plutôt que d'élargir la colonne existante.
- **Impression soignée** — feuille de style dédiée à l'impression de l'emploi du temps (voir §3),
  distincte du rendu écran.
- **Micro-animations pensées au-delà de celles fournies par AtlantaFX** — le README note
  explicitement l'absence d'animations custom ; un travail ciblé (transition d'ouverture de
  dialogue, apparition des cartes) pourrait renforcer la sensation de qualité sans surcharger.

---

## 6. Pistes — Accessibilité

Le travail de cette session (tooltips, `setAccessibleText`, navigation clavier des créneaux,
focus visible) a posé une première base. Pistes pour aller plus loin :

- **Navigation clavier complète de toute l'application**, pas seulement de la grille d'emploi du
  temps : créer/modifier un cours, gérer les tags, naviguer l'Accueil, tout sans souris.
- **Test réel avec lecteurs d'écran** (NVDA/JAWS sur Windows, VoiceOver sur macOS, Orca sur
  Linux) — le support JavaFX de l'accessibilité est connu pour être inégal selon l'OS ; les
  `setAccessibleText` ajoutés sont une base nécessaire mais pas une garantie de fonctionnement
  réel sans validation.
- **Vérification WCAG AA des contrastes** sur les deux thèmes (clair/sombre) et sur les couleurs
  de cours choisies librement par l'utilisateur (une couleur de cours très claire sur fond clair
  pourrait devenir illisible — actuellement aucun garde-fou).
- **Distinction non-couleur des cours** — motif, icône ou lettre en plus de la couleur sur les
  blocs de créneaux, pour les utilisateurs daltoniens (les couleurs de cours sont actuellement le
  seul signal visuel de distinction dans la grille).
- **Respect des préférences système** — réduction des animations si `prefers-reduced-motion`
  (ou équivalent JavaFX/OS) est activé.
- **Zoom d'interface** (voir aussi §4) au service à la fois du confort en vidéoprojection et de la
  basse vision.

---

## 7. Pistes — CLI `lecture`

- **Autocomplétion shell** (bash/zsh/fish) pour les sous-commandes, `--day`, noms de cours/fichiers
  — gain d'ergonomie important pour un outil pensé pour un usage répété au clavier.
- **Sortie structurée (`--json`)** sur les commandes de consultation (`slot`, `slots`, `courses`,
  `course`), pour permettre le scripting/l'intégration avec d'autres outils (ex. afficher le
  prochain cours dans une barre de statut type `polybar`/`waybar`).
- **Notification desktop native depuis la CLI** (`libnotify` sur Linux, équivalents macOS/Windows)
  plutôt que seulement une sortie texte dans le terminal.
- **Mode "watch"** qui réaffiche automatiquement `lecture slot` à chaque changement de créneau,
  utile affiché en permanence dans un coin d'écran/une barre de statut.
- **Alias/raccourcis configurables** — permettre à l'utilisateur de définir ses propres
  raccourcis de sous-commandes dans un fichier de config CLI.
- **`lecture week` plus riche** une fois les semaines cycliques à N (§3) implémentées.

---

## 8. Pistes — Architecture, qualité technique, expérience développeur

- **Tests automatisés sur `app`** — [TestFX](https://github.com/TestFX/TestFX) est l'option de
  référence pour tester des panes JavaFX ; commencer par les parcours critiques (créer un cours,
  créer un créneau, undo/redo) comblerait le plus grand angle mort actuel du projet (0 test sur
  3550 lignes UI).
- **CI (GitHub Actions ou équivalent)** — build + `mvn test` à chaque push/PR, pour que la
  robustesse déjà présente sur `core` cesse de dépendre de la discipline manuelle. Base simple
  vu qu'il n'y a qu'un seul JDK/Maven à installer.
- **Analyse statique** — SpotBugs/PMD/Checkstyle (ou l'équivalent moderne, Error Prone) en étape
  de CI, pour attraper les régressions de qualité avant la revue humaine.
- **Mesure de couverture de tests** (JaCoCo) pour objectiver où porter l'effort de test en
  priorité plutôt que de deviner.
- **Découpage des panes volumineuses** — `EmploiDuTempsPane` et `CoursGestionPane` mélangent
  construction d'UI, état et logique métier dans des classes de 900-1000+ lignes ; extraire des
  sous-composants (ex. le dialogue de créneau, la cellule de fichier) en classes dédiées
  faciliterait à la fois les tests et la lecture.
- **Version de schéma explicite sur `data.json`** — un champ `version` + une chaîne de migrations
  documentées, plutôt que de compter implicitement sur la tolérance de Jackson aux champs
  inconnus, pour absorber sereinement de futurs changements de modèle (ex. la piste "fichier
  multi-cours" de §3).
- **Documentation technique générée** (Javadoc publié, ou un diagramme d'architecture simple)
  pour abaisser le coût d'entrée si le projet accueille un jour d'autres contributeurs.
- **`CONTRIBUTING.md` + gabarits d'issue/PR** — utile dès que le projet sort du cadre solo (voir
  §11), même si ce n'est pas une priorité tant qu'il reste personnel.
- **Découpage plus poussé du module `core`** si la CLI grossit beaucoup (ex. séparer `cli` en son
  propre module Maven) — à ne considérer que si `core` devient lui-même difficile à naviguer,
  pas préventivement.

---

## 9. Pistes — Données, sauvegarde et synchronisation

- **Chiffrement optionnel de `data.json` au repos** — pertinent si des données plus sensibles
  s'ajoutent un jour (ex. des notes liées à des élèves) ; pas nécessaire aujourd'hui vu la nature
  des données actuelles (cours, fichiers, horaires).
- **Synchronisation multi-appareils** — déjà notée en Phase 7 de la Roadmap comme idée non
  développée. Deux approches très différentes à trancher le moment venu :
  - *Léger* : synchroniser `data.json` via un dossier déjà synchronisé par l'utilisateur
    (Nextcloud/Dropbox/Google Drive local), avec une résolution de conflit simple
    (horodatage/dernier gagnant) — peu de code, mais fragile en cas d'écriture concurrente.
  - *Robuste* : un vrai service de synchronisation (compte, résolution de conflits structurée,
    historique) — beaucoup plus de travail, mais la seule option fiable pour un usage multi-appareil
    quotidien.
- **Export standards** — iCal pour l'emploi du temps (§3), CSV pour la liste des cours/fichiers,
  utiles indépendamment d'une vraie synchronisation.
- **Snapshots consultables** — remplacer/compléter la rotation actuelle (3 `.bak` silencieux) par
  un historique de versions explicite et consultable depuis l'UI (à la manière d'un "historique
  des versions" façon Google Docs, même basique).
- **Mode portable** — lancer l'application avec ses données depuis une clé USB (`data.json` à
  côté de l'exécutable plutôt que dans `~/.courseflow`), pour un enseignant changeant souvent de
  poste.
- **Sauvegarde cloud automatique chiffrée** — au-delà de l'export manuel actuel dans les
  Paramètres, un envoi périodique optionnel vers un service choisi par l'utilisateur.

---

## 10. Pistes — Packaging et déploiement multiplateforme

- **Build Windows** (`.msi`/`.exe` via `jpackage --type msi/exe`) — la Roadmap le note déjà comme
  manquant, nécessite une machine Windows (ou un runner CI Windows) pour le build.
- **Build macOS** (`.dmg`/`.pkg`), avec la contrainte supplémentaire de la notarization Apple
  (compte développeur payant, signature de code) pour éviter l'avertissement "développeur non
  identifié" à l'ouverture.
- **`.rpm`** pour Fedora/RHEL/openSUSE, en plus du `.deb` déjà fonctionnel — `jpackage` le
  supporte nativement, il manque juste `rpmbuild` sur la machine de build (noté dans le README
  comme la seule raison de son absence actuelle).
- **Flatpak/Snap** — packaging Linux universel indépendant de la distribution, pertinent si la
  diffusion dépasse un cercle Debian/Ubuntu.
- **Homebrew cask (macOS)** et **Winget/Chocolatey (Windows)** — canaux d'installation attendus
  par les utilisateurs techniques de ces plateformes, en complément des installeurs natifs.
- **CI de build multiplateforme** (matrice GitHub Actions Linux/macOS/Windows) pour fabriquer les
  trois installeurs à chaque release, plutôt qu'à la main sur trois machines différentes.
- **Mécanisme de mise à jour intégré** — aujourd'hui une nouvelle version demande une réinstallation
  manuelle complète ; un mécanisme "vérifier les mises à jour" (même basique, un lien vers la
  dernière release) réduirait la friction pour rester à jour.
- **Signature de code** (Authenticode Windows, notarization macOS) — condition pour que les futurs
  installeurs Windows/macOS ne déclenchent pas d'avertissement de sécurité par défaut.
- **Changelog généré automatiquement** à partir des messages de commit/PR pour chaque release.

---

## 11. Pistes — Distribution, écosystème, communauté

*Cette section suppose un horizon où CourseFlow s'ouvrirait au-delà d'un usage personnel — rien
n'indique que ce soit l'intention actuelle, elle est incluse parce que la demande porte
explicitement sur le "très long terme".*

- **Documentation utilisateur séparée de la documentation technique** — le README actuel mélange
  très bien les deux pour un lecteur développeur, mais un enseignant non technique bénéficierait
  d'un guide illustré, sans jargon Maven/JPMS.
- **Traduction anglaise** (voir aussi §13 sur l'i18n) — élargirait considérablement l'audience
  potentielle au-delà du monde francophone.
- **Site vitrine minimal** — une page présentant le problème résolu et des captures d'écran, pour
  quiconque découvre le projet sans lire de code.
- **Système de plugins/extensions** — pour des intégrations tierces (types de fichiers exotiques,
  connecteurs vers d'autres outils) sans faire grossir le cœur de l'application indéfiniment.
  Gros chantier d'architecture, à n'envisager que si des besoins d'extension récurrents émergent.
- **Bibliothèque de modèles de cours partageables** — un enseignant pourrait exporter/partager un
  squelette de cours (structure de tags, blocs horaires type) avec un collègue, sans partager ses
  fichiers personnels.
- **Télémétrie strictement opt-in et anonymisée** — pour prioriser objectivement les prochaines
  fonctionnalités selon l'usage réel plutôt que l'intuition, uniquement si explicitement consentie.
- **Canal de retour utilisateur** (issues GitHub, formulaire) — actuellement aucun mécanisme de
  feedback structuré n'existe (cohérent avec un projet solo, mais à prévoir si ça change).

---

## 12. Pistes — Sécurité et confidentialité

- **Revue et tests explicites de la sécurité d'ouverture de fichiers** — `OuvreurFichiers` utilise
  déjà `ProcessBuilder` avec une liste d'arguments (pas de concaténation shell, donc pas
  d'injection de commande via un nom de fichier piégé), mais aucun test unitaire ne le vérifie
  explicitement ; un test de non-régression sur ce point précis serait peu coûteux et rassurant.
- **Avertissement avant ouverture d'un lien web non-HTTPS** — `OuvreurFichiers.estUrl` accepte
  aujourd'hui n'importe quel schéma d'URL valide ; un avertissement pour les schémas inhabituels
  (ni `http`/`https`) réduirait le risque d'ouverture accidentelle d'un lien inattendu.
- **Permissions restrictives sur `~/.courseflow/data.json`** — à vérifier/forcer explicitement
  (lecture/écriture réservées à l'utilisateur), surtout si des données plus sensibles s'y ajoutent
  un jour (voir §9).
- **Verrouillage local optionnel** (mot de passe/code, ou intégration biométrique OS) — pertinent
  uniquement si l'application venait à stocker des données sensibles sur des élèves ; pas
  justifié par le contenu actuel (cours, fichiers, horaires).
- **Revue de la chaîne de mise à jour** une fois un mécanisme d'auto-update ajouté (§10) — vérifier
  l'intégrité des paquets téléchargés (signature) pour éviter qu'un mécanisme d'update devienne
  lui-même un vecteur d'attaque.

---

## 13. Vision long terme (horizon incertain)

Idées qui dépassent largement le cadre actuel (application de bureau mono-utilisateur) — à ne lire
que comme un exercice d'imagination sur "jusqu'où CourseFlow pourrait aller", pas comme une
intention.

- **Compagnon mobile en lecture seule** — consulter son emploi du temps et ouvrir un fichier
  (stocké dans le cloud) depuis un téléphone, en complément de l'application de bureau qui reste
  l'outil principal.
- **Internationalisation complète** — aujourd'hui, tout le texte est du français codé en dur
  (`NomsJours`, tous les libellés UI/CLI) ; une vraie couche i18n (`ResourceBundle` ou équivalent)
  serait un chantier transversal touchant la quasi-totalité des classes UI et CLI.
- **Recherche sémantique dans le contenu des fichiers** — indexer le texte des PDF/documents pour
  chercher "le contrôle sur les fractions" plutôt que de se souvenir du nom exact du fichier.
- **Suggestions intelligentes de fichiers** — à partir de l'historique d'usage (§4), suggérer les
  fichiers probables pour une nouvelle séance d'un cours récurrent.
- **Mode "équipe pédagogique"** — plusieurs enseignants d'un même établissement partageant une
  progression ou des ressources communes. Impliquerait de repenser entièrement le modèle
  mono-utilisateur actuel (comptes, permissions, synchronisation multi-utilisateur) — un
  changement d'échelle du projet, pas une extension.
- **Tableau de bord "vue d'ensemble de l'année"** — progression pédagogique visualisée sur un
  temps long (trimestre/année), au-delà de la semaine type actuelle.
- **Intégration avec un cahier de textes numérique officiel** (Pronote ou autre ENT), si une API
  exploitable existe côté éditeur — au-delà du simple import ponctuel évoqué en §3.
- **Support tablette + stylet pour annoter les PDF directement** — proche d'un produit à part
  entière (type GoodNotes/Notability) ; pertinent seulement si l'usage réel montre un besoin
  d'annotation, pas de simple consultation.
- **Widget d'écran de verrouillage / intégrations Siri-Google Assistant** — "quel est mon prochain
  cours ?" en une commande vocale, une fois une brique mobile/cloud en place.

---

## 14. Comment prioriser tout ça

Le README le dit déjà bien pour les phases 6-8 : prioriser "selon l'usage réel de l'application
plutôt qu'en amont". Ce principe s'applique encore plus à ce document, volontairement large.
Quelques repères pour trier, le moment venu, sans prétendre remplacer un vrai arbitrage :

1. **Ce qui comble un angle mort déjà identifié pèse plus qu'une nouvelle capacité** — les tests
   `app` (§8) et l'absence de CI en sont l'exemple le plus net : ils ne changent rien à l'usage
   quotidien, mais toute évolution future devient plus risquée tant qu'ils manquent.
2. **Ce qui découle d'une gêne vécue plusieurs fois pèse plus qu'une idée séduisante mais non
   éprouvée** — exactement la logique déjà appliquée pour bâtir ce projet (README : "à prioriser
   selon l'usage réel").
3. **Les chantiers qui changent le modèle de données** (semaines à N, fichier multi-cours, mode
   multi-utilisateur) coûtent plus cher à faire tard qu'à faire tôt si on sait déjà qu'on les
   veut — mais ne valent la peine d'être anticipés que si le besoin est confirmé, pas par
   précaution seule.
- **Aucune de ces pistes n'est un prérequis à une autre**, à quelques exceptions logiques près
  explicitement notées ci-dessus (ex. mobile avant Siri/Assistant, i18n avant traduction
  anglaise diffusée).
