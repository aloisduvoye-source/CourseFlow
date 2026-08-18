package com.teacherflow.cli;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.persistence.DataStore;
import com.teacherflow.util.NomsJours;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Point d'entrée CLI headless (sans interface graphique) : ouvre les fichiers du créneau
 * courant, ou d'un créneau ciblé via {@code --jour}/{@code --heure}.
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
            System.err.println("Usage : lecture [--jour <Lundi|Mardi|...>] [--heure HH:mm]");
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

        Optional<Creneau> creneauCourant = emploiDuTemps.trouverCreneauCourant(arguments.getJour(), arguments.getHeure());
        if (creneauCourant.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                    + " à " + arguments.getHeure() + ".");
            return;
        }

        Creneau creneau = creneauCourant.get();
        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        String nomCours = cours != null ? cours.getNom() : "(cours supprimé)";
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);

        if (fichiers.isEmpty()) {
            System.out.println("Aucun fichier sélectionné pour ce créneau (" + nomCours + ").");
            return;
        }

        System.out.println("Ouverture de " + fichiers.size() + " fichier(s) pour \"" + nomCours + "\"...");
        List<String> echecs = OuvreurFichiers.ouvrir(fichiers);
        if (!echecs.isEmpty()) {
            System.err.println("Fichiers introuvables ou non ouvrables : " + String.join(", ", echecs));
            System.exit(1);
        }
    }
}
