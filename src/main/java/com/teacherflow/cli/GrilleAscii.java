package com.teacherflow.cli;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.util.NomsJours;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.Optional;

/**
 * Construit une représentation ASCII de l'emploi du temps de la semaine (une grille jours ×
 * heures), pour affichage dans un terminal via l'option {@code -s} de la commande {@code lecture}.
 */
public final class GrilleAscii {

    private static final DayOfWeek[] JOURS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private static final int HEURE_DEBUT = 7;
    private static final int HEURE_FIN = 20;
    private static final int LARGEUR_CONTENU = 11;
    private static final int LARGEUR_LABEL = 6;

    private GrilleAscii() {
    }

    /**
     * @param aujourdhui le jour à marquer d'un astérisque dans l'en-tête (typiquement le jour système).
     */
    public static String construire(EmploiDuTemps emploiDuTemps, DayOfWeek aujourdhui) {
        StringBuilder texte = new StringBuilder();

        texte.append(" ".repeat(LARGEUR_LABEL));
        for (DayOfWeek jour : JOURS) {
            String nom = NomsJours.nom(jour) + (jour == aujourdhui ? "*" : "");
            texte.append("| ").append(centrer(nom, LARGEUR_CONTENU));
        }
        texte.append('\n');
        texte.append("-".repeat(LARGEUR_LABEL + JOURS.length * (LARGEUR_CONTENU + 2)));
        texte.append('\n');

        for (int heure = HEURE_DEBUT; heure < HEURE_FIN; heure++) {
            texte.append(String.format("%02d:00 ", heure));
            for (DayOfWeek jour : JOURS) {
                String contenu = trouverContenu(emploiDuTemps, jour, heure);
                texte.append("| ").append(String.format("%-" + LARGEUR_CONTENU + "s", contenu));
            }
            texte.append('\n');
        }

        return texte.toString();
    }

    private static String trouverContenu(EmploiDuTemps emploiDuTemps, DayOfWeek jour, int heure) {
        Optional<Creneau> creneau = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == jour)
                .filter(c -> couvreHeure(c, heure))
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .findFirst();

        if (creneau.isEmpty()) {
            return "";
        }
        Cours cours = emploiDuTemps.trouverCours(creneau.get().getCoursId()).orElse(null);
        String nom = cours != null ? cours.getNom() : "?";
        return tronquer(nom, LARGEUR_CONTENU);
    }

    private static boolean couvreHeure(Creneau creneau, int heure) {
        int debut = creneau.getHeureDebut().getHour() * 60 + creneau.getHeureDebut().getMinute();
        int fin = creneau.getHeureFin().getHour() * 60 + creneau.getHeureFin().getMinute();
        int fenetreDebut = heure * 60;
        int fenetreFin = (heure + 1) * 60;
        return debut < fenetreFin && fin > fenetreDebut;
    }

    private static String tronquer(String texte, int largeur) {
        if (texte.length() <= largeur) {
            return texte;
        }
        return texte.substring(0, Math.max(0, largeur - 1)) + "…";
    }

    private static String centrer(String texte, int largeur) {
        String tronque = tronquer(texte, largeur);
        int espaceTotal = largeur - tronque.length();
        int gauche = espaceTotal / 2;
        int droite = espaceTotal - gauche;
        return " ".repeat(Math.max(0, gauche)) + tronque + " ".repeat(Math.max(0, droite));
    }
}
