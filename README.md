# CourseFlow

**Application de bureau (JavaFX) qui organise les fichiers de cours d'un enseignant autour de
son emploi du temps.**

Un enseignant réutilise en grande partie les mêmes supports d'une semaine sur l'autre pour un
même cours. CourseFlow attache une bibliothèque de fichiers à chaque **Cours**, les répartit dans
un **emploi du temps** type, et ouvre en un clic (ou une commande `lecture`) exactement les
fichiers du créneau en cours — plus jamais de temps perdu à chercher le bon PDF avant d'entrer en
classe.

## Documentation

| Document | Contenu |
|---|---|
| **[Guide utilisateur](doc/GUIDE_UTILISATEUR.md)** | Le problème, les concepts, l'utilisation de l'interface graphique et de la commande `lecture`, l'installation |
| **[Guide développeur](doc/GUIDE_DEVELOPPEUR.md)** | Stack technique, architecture, build & tests, packaging/distribution, état d'avancement détaillé |
| **[Bilan & pistes d'amélioration](doc/BILAN_ET_PISTES.md)** | État des lieux complet du projet et inventaire large des évolutions possibles, court comme très long terme |

## Démarrage rapide (développement)

```
mvn -pl core -am install -DskipTests
mvn -f app/pom.xml javafx:run
```

Prérequis, tests, construction d'un paquet installable (`.deb`) : voir le
[Guide développeur](doc/GUIDE_DEVELOPPEUR.md).

## Stack

Java 21 · JavaFX 21 + [AtlantaFX](https://github.com/mkpaz/atlantafx) · Maven multi-module
(`core` headless / `app` graphique) · persistance JSON locale (Jackson)

## État

Le parcours principal — créer des cours, les remplir de fichiers, les placer dans l'emploi du
temps, choisir les fichiers par séance, et les ouvrir en un clic ou une commande — est complet et
utilisable au quotidien. Détail phase par phase dans le
[Guide développeur](doc/GUIDE_DEVELOPPEUR.md#état-davancement--roadmap).
