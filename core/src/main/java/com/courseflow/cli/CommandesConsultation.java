package com.courseflow.cli;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.TypeSemaine;
import com.courseflow.util.NomsJours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Sous-commandes qui affichent des informations sans rien ouvrir : {@code slot}, {@code slots}, {@code courses}, {@code course}. */
final class CommandesConsultation {

    private CommandesConsultation() {
    }

    static void slot(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneau = OutilsLecture.resoudreCreneauCible(emploiDuTemps, arguments);
        if (creneau.isEmpty()) {
            System.out.println(OutilsLecture.messageAucunCreneau(arguments));
            return;
        }
        afficherSlot(emploiDuTemps, creneau.get());
    }

    private static void afficherSlot(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        System.out.println("Créneau : " + NomsJours.nom(creneau.getJour()) + " "
                + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
        System.out.println("Cours : " + OutilsLecture.nomCours(emploiDuTemps, creneau));
        if (creneau.getSalle() != null && !creneau.getSalle().isBlank()) {
            System.out.println("Salle : " + creneau.getSalle());
        }
        if (creneau.getDescription() != null && !creneau.getDescription().isBlank()) {
            System.out.println("Description : " + creneau.getDescription());
        }

        System.out.println("Fichiers :");
        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        if (fichiers.isEmpty()) {
            System.out.println("  (aucun fichier sélectionné pour ce créneau)");
            return;
        }
        for (Fichier fichier : fichiers) {
            System.out.println("  " + OutilsLecture.libelleFichier(fichier));
        }
    }

    static void slotsDuJour(EmploiDuTemps emploiDuTemps, LocalDate date) {
        DayOfWeek jour = date.getDayOfWeek();
        TypeSemaine semaine = emploiDuTemps.getParametres().semainePour(date);
        List<Creneau> creneaux = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == jour && c.correspondA(semaine))
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .collect(Collectors.toList());

        if (creneaux.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(jour) + ".");
            return;
        }

        System.out.println("Créneaux de " + NomsJours.nom(jour) + " :");
        for (Creneau creneau : creneaux) {
            System.out.println("  " + creneau.getHeureDebut() + " - " + creneau.getHeureFin()
                    + "  " + OutilsLecture.nomCours(emploiDuTemps, creneau));
        }
    }

    static void listerCours(EmploiDuTemps emploiDuTemps, boolean missingInfoSeulement) {
        List<Cours> cours = emploiDuTemps.getCours().stream()
                .sorted(Comparator.comparing(Cours::getNom, String.CASE_INSENSITIVE_ORDER))
                .filter(c -> !missingInfoSeulement || aucunCreneau(emploiDuTemps, c))
                .toList();

        if (missingInfoSeulement) {
            if (cours.isEmpty()) {
                System.out.println("Tous les cours ont au moins un créneau planifié.");
                return;
            }
            System.out.println("Cours sans créneau planifié :");
        } else {
            if (cours.isEmpty()) {
                System.out.println("Aucun cours.");
                return;
            }
            System.out.println("Cours :");
        }

        for (Cours c : cours) {
            int nbFichiers = c.getFichiers().size();
            System.out.println("  " + c.getNom() + " (" + nbFichiers + " fichier" + (nbFichiers > 1 ? "s" : "") + ")");
        }
    }

    private static boolean aucunCreneau(EmploiDuTemps emploiDuTemps, Cours cours) {
        return emploiDuTemps.getCreneaux().stream().noneMatch(c -> c.getCoursId().equals(cours.getId()));
    }

    static void afficherCours(EmploiDuTemps emploiDuTemps, String nomCours) {
        Optional<Cours> coursOpt = OutilsLecture.trouverCoursParNom(emploiDuTemps, nomCours);
        if (coursOpt.isEmpty()) {
            System.err.println("Aucun cours nommé \"" + nomCours + "\".");
            System.exit(1);
            return;
        }

        Cours cours = coursOpt.get();
        System.out.println("Cours : " + cours.getNom());
        if (cours.getCouleur() != null && !cours.getCouleur().isBlank()) {
            System.out.println("Couleur : " + cours.getCouleur());
        }

        System.out.println("Fichiers :");
        if (cours.getFichiers().isEmpty()) {
            System.out.println("  (aucun fichier dans la bibliothèque de ce cours)");
        } else {
            for (Fichier fichier : cours.getFichiers()) {
                System.out.println("  " + OutilsLecture.libelleFichier(fichier));
            }
        }

        List<Creneau> creneaux = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getCoursId().equals(cours.getId()))
                .sorted(Comparator.comparingInt((Creneau c) -> c.getJour().getValue())
                        .thenComparing(Creneau::getHeureDebut))
                .toList();
        System.out.println("Créneaux :");
        if (creneaux.isEmpty()) {
            System.out.println("  (aucun créneau planifié pour ce cours)");
        } else {
            for (Creneau creneau : creneaux) {
                System.out.println("  " + NomsJours.nom(creneau.getJour()) + " "
                        + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
            }
        }
    }
}
