package com.teacherflow.cli;

import com.teacherflow.io.OuvreurFichiers;
import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.model.TypeSemaine;
import com.teacherflow.persistence.DataStore;
import com.teacherflow.util.NomsJours;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Point d'entrée CLI headless (sans interface graphique) : ouvre ou consulte les fichiers et
 * informations liés aux créneaux/cours de l'emploi du temps, via une sous-commande optionnelle
 * ({@code slot}, {@code slots}, {@code schedule}, {@code courses}, {@code course},
 * {@code open-file}). Sans sous-commande, ouvre les fichiers du créneau ciblé (courant, ou via
 * {@code --next}/{@code --previous}/{@code --day}/{@code --date}+{@code --time}).
 * {@code lecture .} lance l'interface graphique (interceptée par {@code bin/lecture} avant
 * d'atteindre cette classe).
 */
public final class Lecture {

    private Lecture() {
    }

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--help")) {
            afficherAide();
            return;
        }

        ArgumentsLecture arguments;
        try {
            arguments = ArgumentsLecture.analyser(args, LocalDate.now(), LocalTime.now());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage : lecture [COMMAND] [OPTIONS] (voir lecture --help)");
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

        switch (arguments.getCommande()) {
            case OUVRIR -> traiterOuverture(emploiDuTemps, arguments);
            case SLOT -> traiterSlot(emploiDuTemps, arguments);
            case SLOTS -> listerCreneauxDuJour(emploiDuTemps, arguments.getDate());
            case SCHEDULE -> System.out.print(GrilleAscii.construire(emploiDuTemps, LocalDate.now()));
            case COURSES -> listerCours(emploiDuTemps, arguments.isMissingInfo());
            case COURSE -> afficherCours(emploiDuTemps, arguments.getNomCours());
            case OPEN_FILE -> ouvrirFichierCible(emploiDuTemps, arguments);
        }
    }

    private static void afficherAide() {
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

                  lecture .""");
    }

    private static void traiterOuverture(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneau = resoudreCreneauCible(emploiDuTemps, arguments);
        if (creneau.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                    + " à " + formatHeure(arguments.getHeure()) + ".");
            return;
        }
        ouvrirCreneau(emploiDuTemps, creneau.get());
    }

    private static void traiterSlot(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneau = resoudreCreneauCible(emploiDuTemps, arguments);
        if (creneau.isEmpty()) {
            System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                    + " à " + formatHeure(arguments.getHeure()) + ".");
            return;
        }
        afficherSlot(emploiDuTemps, creneau.get());
    }

    /**
     * Résout le créneau ciblé par {@code arguments} : courant (jour/heure), ou précédent/suivant
     * via {@link NavigationCreneaux} à partir de ce même point de référence. Avertit si le
     * créneau résolu via {@code --next}/{@code --previous} tombe un autre jour que la référence.
     */
    private static Optional<Creneau> resoudreCreneauCible(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
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

    private static void afficherSlot(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        String nomCours = nomCours(emploiDuTemps, creneau);

        System.out.println("Créneau : " + NomsJours.nom(creneau.getJour()) + " "
                + creneau.getHeureDebut() + " - " + creneau.getHeureFin());
        System.out.println("Cours : " + nomCours);
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
            System.out.println("  " + libelleFichier(fichier));
        }
    }

    private static void listerCreneauxDuJour(EmploiDuTemps emploiDuTemps, LocalDate date) {
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
            String nomCours = nomCours(emploiDuTemps, creneau);
            System.out.println("  " + creneau.getHeureDebut() + " - " + creneau.getHeureFin() + "  " + nomCours);
        }
    }

    private static void listerCours(EmploiDuTemps emploiDuTemps, boolean missingInfoSeulement) {
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

    private static void afficherCours(EmploiDuTemps emploiDuTemps, String nomCours) {
        Optional<Cours> coursOpt = trouverCoursParNom(emploiDuTemps, nomCours);
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
                System.out.println("  " + libelleFichier(fichier));
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

    private static void ouvrirFichierCible(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        List<Fichier> fichiersDisponibles;
        String contexte;

        if (arguments.getNomCours() != null) {
            Optional<Cours> coursOpt = trouverCoursParNom(emploiDuTemps, arguments.getNomCours());
            if (coursOpt.isEmpty()) {
                System.err.println("Aucun cours nommé \"" + arguments.getNomCours() + "\".");
                System.exit(1);
                return;
            }
            fichiersDisponibles = coursOpt.get().getFichiers();
            contexte = "le cours \"" + coursOpt.get().getNom() + "\"";
        } else {
            Optional<Creneau> creneauOpt = emploiDuTemps.trouverCreneauCourant(arguments.getDate(), arguments.getHeure());
            if (creneauOpt.isEmpty()) {
                System.out.println("Aucun créneau prévu " + NomsJours.nom(arguments.getJour())
                        + " à " + formatHeure(arguments.getHeure()) + ".");
                return;
            }
            fichiersDisponibles = emploiDuTemps.fichiersPourCreneau(creneauOpt.get());
            contexte = "ce créneau";
        }

        Optional<Fichier> fichierOpt = fichiersDisponibles.stream()
                .filter(f -> libelleFichier(f).equalsIgnoreCase(arguments.getNomFichier()))
                .findFirst();

        if (fichierOpt.isEmpty()) {
            System.err.println("Aucun fichier nommé \"" + arguments.getNomFichier() + "\" pour " + contexte + ".");
            if (!fichiersDisponibles.isEmpty()) {
                String disponibles = fichiersDisponibles.stream()
                        .map(Lecture::libelleFichier)
                        .collect(Collectors.joining(", "));
                System.err.println("Fichiers disponibles : " + disponibles);
            }
            System.exit(1);
            return;
        }

        List<String> echecs = OuvreurFichiers.ouvrir(List.of(fichierOpt.get()));
        if (!echecs.isEmpty()) {
            System.err.println("Fichier introuvable ou non ouvrable : " + String.join(", ", echecs));
            System.exit(1);
            return;
        }
        System.out.println("Ouverture de \"" + libelleFichier(fichierOpt.get()) + "\"...");
    }

    private static Optional<Cours> trouverCoursParNom(EmploiDuTemps emploiDuTemps, String nom) {
        return emploiDuTemps.getCours().stream()
                .filter(c -> c.getNom() != null && c.getNom().equalsIgnoreCase(nom))
                .findFirst();
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

    private static String libelleFichier(Fichier fichier) {
        return fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank()
                ? fichier.getNomAffichage() : fichier.getChemin();
    }

    /**
     * @return l'heure tronquée à la minute (sans les secondes/nanosecondes que
     * {@link LocalTime#now()} peut inclure, non pertinentes dans un message affiché).
     */
    private static LocalTime formatHeure(LocalTime heure) {
        return heure.withSecond(0).withNano(0);
    }
}
