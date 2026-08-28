package com.courseflow.cli;

import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.TypeSemaine;
import com.courseflow.persistence.DataStore;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Sous-commande {@code week} : affiche ou règle la semaine courante de l'alternance A/B. */
final class CommandeSemaine {

    private CommandeSemaine() {
    }

    /**
     * Affiche la semaine courante (A/B), ou la change via {@code --set} : dans ce cas, règle la
     * date d'ancrage ({@link com.courseflow.model.Parametres#setAncrageSemaineA}) sur le lundi
     * de la semaine courante (pour {@code --set a}) ou celui de la semaine précédente (pour
     * {@code --set b}), de façon à ce que la semaine actuelle devienne celle demandée. Utile pour
     * corriger l'alternance quand elle a été décalée (ex. par des vacances).
     */
    static void traiter(DataStore dataStore, EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        var parametres = emploiDuTemps.getParametres();

        if (arguments.getSemaineVoulue() != null) {
            LocalDate lundiCourant = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate nouvelAncrage = arguments.getSemaineVoulue() == TypeSemaine.A
                    ? lundiCourant : lundiCourant.minusWeeks(1);
            parametres.setAncrageSemaineA(nouvelAncrage);
            try {
                dataStore.sauvegarder(emploiDuTemps);
            } catch (IOException e) {
                System.err.println("Impossible d'enregistrer dans " + dataStore.getFichierDonnees()
                        + " : " + e.getMessage());
                System.exit(1);
                return;
            }
            System.out.println("Semaine actuelle réglée sur " + arguments.getSemaineVoulue()
                    + " (date de référence : " + nouvelAncrage + ").");
            return;
        }

        TypeSemaine semaine = parametres.semainePour(LocalDate.now());
        System.out.println("Semaine actuelle : " + semaine);
        if (parametres.getAncrageSemaineA() == null) {
            System.out.println("(Aucune date de référence définie dans les Paramètres : toutes les semaines "
                    + "sont considérées comme la semaine A tant qu'elle n'est pas réglée, via l'application ou "
                    + "\"lecture week --set a/b\".)");
        }
    }
}
