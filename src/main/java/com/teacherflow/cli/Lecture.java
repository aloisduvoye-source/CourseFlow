package com.teacherflow.cli;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.persistence.DataStore;
import com.teacherflow.util.NomsJours;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Point d'entrée CLI headless (sans interface graphique) : ouvre les fichiers du créneau
 * courant, du créneau ciblé via {@code --jour}/{@code --heure}, ou du créneau précédent/suivant
 * via {@code -p}/{@code -n}. {@code --jour} seul (sans {@code --heure}) liste les créneaux du jour.
 */
public final class Lecture {

    private Lecture() {
    }

    public static void main(String[] args) {
        ArgumentsLecture arguments;
        try {
            arguments = ArgumentsLecture.analyser(args, LocalDate.now().getDayOfWeek(), LocalTime.now());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage : lecture [--jour <Lundi|Mardi|...>] [--heure HH:mm] [-p | -n]");
            System.exit(2);
            return;
        }

        DataStore dataStore = new DataStore();
        EmploiDuTemps emploiDuTemps;
        try {
            emploiDuTemps = dataStore.charger();
        } catch (IOException e) {
            System.err.println("Impossible de charger les données depuis " + dataStore.getFichierDonnees()
                    + " : " + e.getMessage());
            System.exit(1);
            return;
        }

        switch (arguments.getMode()) {
            case LISTE_JOUR -> listerCreneauxDuJour(emploiDuTemps, arguments.getJour());
            case SUIVANT -> ouvrirCreneauRelatif(emploiDuTemps, arguments, true);
            case PRECEDENT -> ouvrirCreneauRelatif(emploiDuTemps, arguments, false);
            case CRENEAU_COURANT -> ouvrirCreneauCourant(emploiDuTemps, arguments);
        }
    }

    private static void listerCreneauxDuJour(EmploiDuTemps emploiDuTemps, DayOfWeek jour) {
        List<Creneau> creneaux = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == jour)
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .collect(Collectors.toList());

        if (creneaux.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(jour) + ".");
            return;
        }

        System.out.println("Créneaux de " + NomsJours.nom(jour) + " :");
        for (Creneau creneau : creneaux) {
            String nomCours = nomCours(emploiDuTemps, creneau);
            System.out.println("  " + creneau.getHeureDebut() + " - " + creneau.getHeureFin() + "  " + nomCours);
        }
    }

    private static void ouvrirCreneauCourant(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneauCourant = emploiDuTemps.trouverCreneauCourant(arguments.getJour(), arguments.getHeure());
        if (creneauCourant.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                    + " à " + arguments.getHeure() + ".");
            return;
        }
        ouvrirCreneau(emploiDuTemps, creneauCourant.get());
    }

    private static void ouvrirCreneauRelatif(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments, boolean suivant) {
        Optional<Creneau> creneau = suivant
                ? NavigationCreneaux.suivant(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure())
                : NavigationCreneaux.precedent(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure());

        if (creneau.isEmpty()) {
            System.out.println("Aucun créneau dans l'emploi du temps.");
            return;
        }
        ouvrirCreneau(emploiDuTemps, creneau.get());
    }

    private static void ouvrirCreneau(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        String nomCours = nomCours(emploiDuTemps, creneau);
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);

        if (fichiers.isEmpty()) {
            System.out.println("Aucun fichier sélectionné pour ce créneau (" + nomCours + ", "
                    + NomsJours.nom(creneau.getJour()) + " " + creneau.getHeureDebut() + ").");
            return;
        }

        System.out.println("Ouverture de " + fichiers.size() + " fichier(s) pour \"" + nomCours + "\" ("
                + NomsJours.nom(creneau.getJour()) + " " + creneau.getHeureDebut() + "-" + creneau.getHeureFin() + ")...");
        List<String> echecs = OuvreurFichiers.ouvrir(fichiers);
        if (!echecs.isEmpty()) {
            System.err.println("Fichiers introuvables ou non ouvrables : " + String.join(", ", echecs));
            System.exit(1);
        }
    }

    private static String nomCours(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        return cours != null ? cours.getNom() : "(cours supprimé)";
    }
}
