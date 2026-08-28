package com.courseflow.cli;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.util.NomsJours;

import java.time.LocalTime;
import java.util.Optional;

/** Helpers partagés par les sous-commandes de {@link Lecture}. */
final class OutilsLecture {

    private OutilsLecture() {
    }

    /**
     * Résout le créneau ciblé par {@code arguments} : courant (jour/heure), ou précédent/suivant
     * via {@link NavigationCreneaux} à partir de ce même point de référence. Avertit sur la sortie
     * standard si le créneau résolu via {@code --next}/{@code --previous} tombe un autre jour que
     * la référence.
     */
    static Optional<Creneau> resoudreCreneauCible(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        if (!arguments.isSuivant() && !arguments.isPrecedent()) {
            return emploiDuTemps.trouverCreneauCourant(arguments.getDate(), arguments.getHeure());
        }

        Optional<Creneau> creneauOpt = arguments.isSuivant()
                ? NavigationCreneaux.suivant(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure())
                : NavigationCreneaux.precedent(emploiDuTemps.getCreneaux(), arguments.getJour(), arguments.getHeure());

        creneauOpt.filter(c -> c.getJour() != arguments.getJour()).ifPresent(creneau ->
                System.out.println("Remarque : ce créneau a lieu " + NomsJours.nom(creneau.getJour())
                        + " (le jour demandé était " + NomsJours.nom(arguments.getJour()) + ")."));

        return creneauOpt;
    }

    static String messageAucunCreneau(ArgumentsLecture arguments) {
        return "Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                + " à " + formatHeure(arguments.getHeure()) + ".";
    }

    static Optional<Cours> trouverCoursParNom(EmploiDuTemps emploiDuTemps, String nom) {
        return emploiDuTemps.getCours().stream()
                .filter(c -> c.getNom() != null && c.getNom().equalsIgnoreCase(nom))
                .findFirst();
    }

    static String nomCours(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        return cours != null ? cours.getNom() : "(cours supprimé)";
    }

    static String libelleFichier(Fichier fichier) {
        return fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                ? fichier.getNomAffichage() : fichier.getChemin();
    }

    /**
     * @return l'heure tronquée à la minute (sans les secondes/nanosecondes que
     * {@link LocalTime#now()} peut inclure, non pertinentes dans un message affiché).
     */
    static LocalTime formatHeure(LocalTime heure) {
        return heure.withSecond(0).withNano(0);
    }
}
