# Guide utilisateur

> Pour l'installation en tant que développeur, les tests, l'architecture et le packaging, voir
> le [Guide développeur](GUIDE_DEVELOPPEUR.md).

## Le problème

Un enseignant réutilise en grande partie les mêmes supports d'une semaine sur l'autre pour un
même cours (ex. "6e A - Mathématiques"), avec des variations ponctuelles selon la séance
(contrôle cette semaine, exercice différent, document ajouté au dernier moment...). Retrouver et
ouvrir ces fichiers avant chaque heure de cours est répétitif et chronophage.

## L'idée

1. On définit des **Cours** réutilisables (ex. "6e A - Mathématiques"), chacun avec sa propre
   bibliothèque de fichiers associés (cahier de texte, supports, exercices, corrections...).
2. On remplit un **emploi du temps** hebdomadaire en assignant un Cours à chaque créneau
   (jour + heure).
3. Pour chaque créneau précis, on choisit — parmi les fichiers du Cours — lesquels sont
   réellement utiles **cette séance-là** (pas besoin de rouvrir tout le dossier du cours à
   chaque fois).
4. Le jour J, un clic dans l'appli (ou une commande terminal, `lecture`) ouvre automatiquement
   les fichiers sélectionnés pour le créneau courant, avec les applications par défaut du
   système.

## Concepts clés

```
Fichier
 ├─ chemin (ou lien web) vers le document
 └─ nom d'affichage optionnel, ex. "Corrigé DS1"

Cours
 ├─ nom (ex. "6e A - Mathématiques")
 ├─ couleur (identification visuelle dans l'emploi du temps)
 └─ bibliothèque de Fichiers rattachés (+ éventuellement des fichiers empruntés à un autre cours)

Créneau
 ├─ jour de la semaine, heure de début/fin
 ├─ Cours associé
 ├─ salle / description (optionnelles)
 └─ sous-ensemble de Fichiers sélectionnés pour CE créneau
    (choisis parmi les fichiers du Cours associé — pas une liste libre)

Emploi du temps
 └─ l'ensemble des Créneaux de la semaine type (récurrente)
```

Point important : les fichiers appartiennent au **Cours**, pas au créneau, qui ne fait que
*piocher* dedans. On attache un fichier une fois à son cours, et on affine juste la sélection
semaine par semaine si besoin — jamais de duplication.

## Utiliser l'interface graphique

La barre latérale donne accès à quatre écrans, et une case en bas bascule le thème clair/sombre.

### Accueil

Écran par défaut au lancement. Affiche l'agenda du jour courant en cartes, une par créneau,
triées par horaire ; le créneau en cours porte un badge "EN COURS". Les flèches ◀ ▶ de part et
d'autre de la liste naviguent jour par jour (l'écran revient toujours au jour réel à chaque
retour sur cet onglet). Chaque carte affiche le cours, l'horaire, la salle et la description si
renseignées, et propose un bouton "▶ Ouvrir N fichiers" qui lance les fichiers de la séance avec
l'application par défaut du système. Le crayon en haut à droite de la carte ouvre directement
l'édition du créneau dans l'onglet Emploi du temps.

### Cours

À gauche : liste des cours existants (recherche par nom) et un bouton "Nouveau cours". Sélectionner
un cours affiche à droite :

- **Nom et couleur** — la couleur identifie le cours dans toute l'application (grille, cartes).
- **Cours par défaut** — désigne un cours de référence dont les autres cours peuvent réutiliser
  des fichiers en un clic (voir ci-dessous).
- **Fichiers du cours** — recherche, ajout (sélecteur de fichiers, dossier entier référencé, ou
  lien web), étiquettes (tags), suppression. Glisser-déposer des fichiers depuis l'explorateur du
  système fonctionne aussi directement sur la liste.
- **Dossiers référencés** — un dossier réel du disque peut être suivi en continu : à chaque
  actualisation, seuls les nouveaux fichiers sont proposés à l'import (rien n'est jamais dupliqué).
- **Lier des fichiers depuis un autre cours** — pour réutiliser la bibliothèque d'un autre cours
  sans dupliquer les fichiers. Le cours désigné "par défaut" est présélectionné par commodité,
  mais n'importe quel cours peut être choisi comme source. Un fichier ainsi lié affiche sa
  provenance ("lié depuis ...") dans la liste.

### Emploi du temps

Grille des jours de la semaine sur la plage horaire configurée (réglable dans Paramètres).
Cliquer sur une case libre crée un nouveau créneau ; cliquer sur un créneau existant l'ouvre pour
modification.

Un créneau placé peut être :
- **déplacé** — glisser son corps à la souris (jour et horaire) ;
- **redimensionné** — glisser un de ses bords haut/bas ;
- **déplacé, ouvert ou supprimé au clavier** — le sélectionner (Tab ou clic), puis flèches pour
  le déplacer, Entrée pour l'ouvrir, Suppr pour le supprimer.

Ctrl+Z / Ctrl+Maj+Z annulent/rétablissent la dernière modification de l'emploi du temps. Le
sélecteur "Semaine A / Semaine B" en haut de l'écran bascule l'affichage entre les deux semaines
d'un emploi du temps en alternance (l'onglet se resynchronise toujours sur la semaine réelle à
chaque ouverture).

Dans la boîte de dialogue d'un créneau : choix du cours (dans le bandeau coloré), horaires,
salle, description, semaine concernée, et une liste à cocher des fichiers du cours à utiliser
pour cette séance précise (tout coché par défaut). Le bouton "▶ Ouvrir maintenant" ouvre
directement les fichiers cochés sans passer par le terminal.

### Paramètres

Regroupés en sections repliables :
- **Jours et grille** — jours affichés, incrément minimal de déplacement, plage horaire de la
  grille.
- **Blocs horaires** — un modèle de créneaux type (ex. 8h-9h puis 9h10-10h10...) dans lequel les
  nouveaux créneaux peuvent être créés ; mêmes interactions souris que la grille principale.
- **Semaines alternées** — date de référence pour l'alternance semaine A / semaine B.
- **Sauvegarde** — export/import de la configuration complète (le fichier `data.json`), pour
  changer de machine ou faire une copie de sûreté.

## Utiliser la commande `lecture`

Lancée dans un terminal, `lecture` fonctionne par sous-commandes (`slot`, `slots`, `schedule`,
`courses`, `course`, `open-file`, `week`) et options longues (`--next`, `--previous`, `--date`,
`--day`, `--time`) :

- sans sous-commande : ouvre les fichiers du créneau ciblé (courant par défaut) ;
- `--day <nom>`/`--date <AAAA-MM-JJ>` + `--time HH:mm` : cible un créneau précis ;
- `--next` / `--previous` : ouvre le créneau **suivant**/**précédent** dans la semaine plutôt que
  celui du moment (navigation circulaire : après le dernier créneau du Dimanche, `--next` revient
  au premier du Lundi, et inversement) — un message signale si ce créneau tombe un autre jour que
  la référence, avant d'ouvrir les fichiers ;
- `slot` : affiche les informations du créneau ciblé (cours, salle, description, fichiers) sans
  rien ouvrir — mêmes options de ciblage que ci-dessus ;
- `slots` : liste tous les créneaux d'un jour (aujourd'hui par défaut, ou via `--day`/`--date`) ;
- `schedule` : affiche l'emploi du temps de la semaine entière en tableau ASCII dans le terminal
  (jour courant marqué d'un `*`) ;
- `courses` : liste tous les cours (avec leur nombre de fichiers) ; `--missing-info` ne liste que
  ceux sans aucun créneau planifié ;
- `course [NOM]` : affiche le détail d'un cours (couleur, bibliothèque complète, créneaux) ;
- `open-file --file [NOM]` : ouvre un fichier précis, dans la bibliothèque d'un cours
  (`--course [NOM]`) ou dans un créneau (`--day`/`--date` + `--time`) ;
- `week` : affiche la semaine actuelle (A/B) ; `week --set a`/`--set b` la règle explicitement
  (utile pour corriger l'alternance après des vacances, par exemple) ;
- `.` : lance l'application graphique elle-même, comme `code .` — rend la main immédiatement,
  sans bloquer le terminal.

La recherche de cours/fichier par nom est exacte (insensible à la casse).

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

lecture week                               # affiche la semaine actuelle (A/B)
lecture week --set a                       # règle la semaine actuelle sur A
```

## Installer CourseFlow

CourseFlow n'est pour l'instant distribué que sous forme de paquet `.deb` à construire soi-même
depuis les sources (Linux uniquement) — voir la section
["Packaging et distribution" du Guide développeur](GUIDE_DEVELOPPEUR.md#packaging-et-distribution)
pour la marche à suivre complète. Une fois le paquet construit :

```
sudo dpkg -i courseflow_1.0.0_amd64.deb
```

L'application (`courseflow`, dans le menu applications) et la commande `lecture` (dans un
terminal) sont alors disponibles immédiatement, sans étape manuelle supplémentaire.

## Données utilisateur

Les données (cours, fichiers, créneaux, paramètres) sont chargées et sauvegardées
automatiquement dans `~/.courseflow/data.json`, à chaque modification et à la fermeture de la
fenêtre — aucune configuration manuelle n'est nécessaire, ce fichier est créé au premier
lancement. Trois copies de secours rotatives (`data.json.bak1`..`bak3`) sont conservées à chaque
sauvegarde, et un fichier illisible est mis de côté plutôt qu'écrasé (l'application le signale et
démarre à vide). Pour changer de machine ou faire une sauvegarde manuelle, utiliser
Export/Import dans Paramètres.
