package com.courseflow.cli;

/** Texte d'aide de la commande {@code lecture} ({@code --help}). */
final class AideLecture {

    private AideLecture() {
    }

    static void afficher() {
        System.out.println("""
                Usage:
                  lecture [OPTIONS]
                  lecture [COMMAND] [OPTIONS]
                  lecture .

                Description:
                  Ouvre et consulte les fichiers et informations liés aux cours.

                Commandes:
                  slot        Afficher les informations d'un créneau
                  slots       Lister les créneaux
                  schedule    Afficher l'emploi du temps de la semaine
                  courses     Lister les cours
                  course      Afficher les informations d'un cours
                  open-file   Ouvrir un fichier spécifique
                  week        Afficher ou changer la semaine actuelle (A/B)

                Arguments spéciaux:
                  .           Ouvrir l'interface graphique

                Options:
                  --next                  Sélectionner le prochain créneau
                  --previous              Sélectionner le créneau précédent
                  --date [DATE]           Sélectionner une date
                  --day [JOUR]            Sélectionner un jour de la semaine
                  --time [HEURE]          Sélectionner une heure
                  --course [COURS]        Sélectionner un cours (avec open-file)
                  --file [FICHIER]        Sélectionner un fichier (avec open-file)
                  --missing-info          Ne lister que les cours sans créneau (avec courses)
                  --set [a|b]             Régler la semaine actuelle sur A ou B (avec week)
                  --help                  Afficher cette aide

                Exemples:
                  lecture
                  lecture --next
                  lecture --previous

                  lecture --date 2026-08-19 --time 10:00
                  lecture --day mercredi --time 10:00

                  lecture slot
                  lecture slot --next
                  lecture slot --date 2026-08-19 --time 10:00

                  lecture slots
                  lecture slots --day mercredi

                  lecture schedule

                  lecture courses
                  lecture courses --missing-info
                  lecture course maths

                  lecture open-file --course maths --file chapitre1.pdf
                  lecture open-file --day mercredi --time 10:00 --file cours.pdf

                  lecture week
                  lecture week --set a
                  lecture week --set b

                  lecture .""");
    }
}
