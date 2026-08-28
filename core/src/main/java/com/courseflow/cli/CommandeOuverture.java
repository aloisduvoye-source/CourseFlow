package com.courseflow.cli;

import com.courseflow.io.OuvreurFichiers;
import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.util.NomsJours;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Sous-commandes qui ouvrent des fichiers : ouverture implicite d'un créneau, et {@code open-file}. */
final class CommandeOuverture {

    private CommandeOuverture() {
    }

    /** Sans sous-commande : ouvre les fichiers du créneau ciblé. */
    static void ouvrirCreneauCible(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        Optional<Creneau> creneau = OutilsLecture.resoudreCreneauCible(emploiDuTemps, arguments);
        if (creneau.isEmpty()) {
            System.out.println(OutilsLecture.messageAucunCreneau(arguments));
            return;
        }
        ouvrirCreneau(emploiDuTemps, creneau.get());
    }

    /** {@code open-file --file NOM} dans un cours ({@code --course}) ou un créneau ({@code --day}/{@code --time}). */
    static void ouvrirFichierCible(EmploiDuTemps emploiDuTemps, ArgumentsLecture arguments) {
        List<Fichier> fichiersDisponibles;
        String contexte;

        if (arguments.getNomCours() != null) {
            Optional<Cours> coursOpt = OutilsLecture.trouverCoursParNom(emploiDuTemps, arguments.getNomCours());
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
                System.out.println(OutilsLecture.messageAucunCreneau(arguments));
                return;
            }
            fichiersDisponibles = emploiDuTemps.fichiersPourCreneau(creneauOpt.get());
            contexte = "ce créneau";
        }

        Optional<Fichier> fichierOpt = fichiersDisponibles.stream()
                .filter(f -> OutilsLecture.libelleFichier(f).equalsIgnoreCase(arguments.getNomFichier()))
                .findFirst();

        if (fichierOpt.isEmpty()) {
            System.err.println("Aucun fichier nommé \"" + arguments.getNomFichier() + "\" pour " + contexte + ".");
            if (!fichiersDisponibles.isEmpty()) {
                String disponibles = fichiersDisponibles.stream()
                        .map(OutilsLecture::libelleFichier)
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
        System.out.println("Ouverture de \"" + OutilsLecture.libelleFichier(fichierOpt.get()) + "\"...");
    }

    private static void ouvrirCreneau(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        String nomCours = OutilsLecture.nomCours(emploiDuTemps, creneau);
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
}
