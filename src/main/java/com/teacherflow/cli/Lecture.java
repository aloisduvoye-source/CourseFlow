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
 * via {@code -p}/{@code -n}. {@code --jour} seul (sans {@code --heure}) liste les créneaux du
 * jour. {@code -l} liste les fichiers du créneau résolu au lieu de les ouvrir.
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
            System.err.println("Usage : lecture [--jour <Lundi|Mardi|...>] [--heure HH:mm] [-p | -n] [-l]");
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
            case SUIVANT -> traiterCreneauRelatif(emploiDuTemps, arguments, true);
            case PRECEDENT -> traiterCreneauRelatif(emploiDuTemps, arguments, false);
            case CRENEAU_COURANT -> traiterCreneauCourant(emploiDuTemps, arguments);
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

    private static void traiterCreneauCourant(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneauCourant = emploiDuTemps.trouverCreneauCourant(arguments.getJour(), arguments.getHeure());
        if (creneauCourant.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                    + " à " + arguments.getHeure() + ".");
            return;
        }
        traiterCreneau(emploiDuTemps, creneauCourant.get(), arguments.isListerFichiers());
    }

    private static void traiterCreneauRelatif(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments, boolean suivant) {
        Optional<Creneau> creneauOpt = suivant
                ? NavigationCreneaux.suivant(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure())
                : NavigationCreneaux.precedent(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure());

        if (creneauOpt.isEmpty()) {
            System.out.println("Aucun créneau dans l'emploi du temps.");
            return;
        }

        Creneau creneau = creneauOpt.get();
        if (creneau.getJour() != arguments.getJour()) {
            System.out.println("Remarque : ce créneau a lieu " + NomsJours.nom(creneau.getJour())
                    + " (le jour demandé était " + NomsJours.nom(arguments.getJour()) + ").");
        }
        traiterCreneau(emploiDuTemps, creneau, arguments.isListerFichiers());
    }

    private static void traiterCreneau(EmploiDuTemps emploiDuTemps, Creneau creneau, boolean listerSeulement) {
        if (listerSeulement) {
            listerFichiersCreneau(emploiDuTemps, creneau);
        } else {
            ouvrirCreneau(emploiDuTemps, creneau);
        }
    }

    private static void listerFichiersCreneau(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        String nomCours = nomCours(emploiDuTemps, creneau);
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);

        System.out.println("Fichiers pour \"" + nomCours + "\" (" + NomsJours.nom(creneau.getJour())
                + " " + creneau.getHeureDebut() + "-" + creneau.getHeureFin() + ") :");
        if (fichiers.isEmpty()) {
            System.out.println("  (aucun fichier sélectionné pour ce créneau)");
            return;
        }
        for (Fichier fichier : fichiers) {
            String libelle = fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                    ? fichier.getNomAffichage() : fichier.getChemin();
            System.out.println("  " + libelle);
        }
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
