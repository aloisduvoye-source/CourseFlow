package com.teacherflow.cli;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import com.teacherflow.util.NomsJours;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit une représentation ASCII de l'emploi du temps de la semaine (une grille jours ×
 * heures), pour affichage dans un terminal via l'option {@code -s} de la commande {@code lecture}.
 * Chaque créneau est dessiné comme une boîte s'étendant sur autant de lignes que sa durée
 * (arrondie à l'heure supérieure), affichant nom (+ salle), description et fichiers dans la
 * limite de la place disponible.
 */
public final class GrilleAscii {

    private static final DayOfWeek[] JOURS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private static final int HEURE_DEBUT = 7;
    private static final int HEURE_FIN = 20;
    private static final int LARGEUR_CONTENU = 14;
    private static final int LARGEUR_LABEL = 6;

    private GrilleAscii() {
    }

    /**
     * @param aujourdhui le jour à marquer d'un astérisque dans l'en-tête (typiquement le jour système).
     */
    public static String construire(EmploiDuTemps emploiDuTemps, DayOfWeek aujourdhui) {
        int nbRangees = HEURE_FIN - HEURE_DEBUT;

        Map<DayOfWeek, Map<Integer, String>> parJour = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek jour : JOURS) {
            parJour.put(jour, construireColonneJour(emploiDuTemps, jour, nbRangees));
        }

        StringBuilder texte = new StringBuilder();
        texte.append(" ".repeat(LARGEUR_LABEL));
        for (DayOfWeek jour : JOURS) {
            String nom = NomsJours.nom(jour) + (jour == aujourdhui ? "*" : "");
            texte.append("| ").append(centrer(nom, LARGEUR_CONTENU));
        }
        texte.append('\n');
        texte.append("-".repeat(LARGEUR_LABEL + JOURS.length * (LARGEUR_CONTENU + 2)));
        texte.append('\n');

        for (int rangee = 0; rangee < nbRangees; rangee++) {
            texte.append(String.format("%02d:00 ", HEURE_DEBUT + rangee));
            for (DayOfWeek jour : JOURS) {
                String cellule = parJour.get(jour).get(rangee);
                texte.append(cellule != null ? cellule : celluleVide());
            }
            texte.append('\n');
        }

        return texte.toString();
    }

    private static Map<Integer, String> construireColonneJour(EmploiDuTemps emploiDuTemps, DayOfWeek jour, int nbRangees) {
        Map<Integer, String> cellules = new HashMap<>();

        List<Creneau> creneaux = emploiDuTemps.getCreneaux().stream()
                .filter(c -> c.getJour() == jour)
                .sorted(Comparator.comparing(Creneau::getHeureDebut))
                .toList();

        for (Creneau creneau : creneaux) {
            int debutMinutes = creneau.getHeureDebut().getHour() * 60 + creneau.getHeureDebut().getMinute();
            int finMinutes = creneau.getHeureFin().getHour() * 60 + creneau.getHeureFin().getMinute();
            int rangeeDebut = (debutMinutes - HEURE_DEBUT * 60) / 60;
            int dureeHeures = Math.max(1, (int) Math.ceil((finMinutes - debutMinutes) / 60.0));
            int rangeeFin = rangeeDebut + dureeHeures;

            rangeeDebut = clamp(rangeeDebut, 0, nbRangees - 1);
            rangeeFin = clamp(rangeeFin, rangeeDebut + 1, nbRangees);
            int hauteur = rangeeFin - rangeeDebut;
            if (hauteur <= 0) {
                continue;
            }

            List<String> contenu = construireContenuBoite(emploiDuTemps, creneau, hauteur);

            for (int i = 0; i < hauteur; i++) {
                int rangee = rangeeDebut + i;
                if (cellules.containsKey(rangee)) {
                    continue;
                }
                char bord = hauteur == 1 ? '│' : (i == 0 ? '┌' : (i == hauteur - 1 ? '└' : '│'));
                String ligne = i < contenu.size() ? contenu.get(i) : "";
                cellules.put(rangee, celluleBoite(bord, ligne));
            }
        }

        return cellules;
    }

    /**
     * Contenu textuel d'une boîte de créneau, limité à {@code hauteurDisponible} lignes :
     * nom (+ salle), puis description si la place le permet, puis autant de fichiers que
     * possible (avec un "+N fichiers" si tous ne tiennent pas).
     */
    private static List<String> construireContenuBoite(EmploiDuTemps emploiDuTemps, Creneau creneau, int hauteurDisponible) {
        List<String> lignes = new ArrayList<>();

        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        String nom = cours != null ? cours.getNom() : "?";
        String salle = creneau.getSalle();
        String ligneNom = (salle != null && !salle.isBlank()) ? nom + " · " + salle : nom;
        lignes.add(tronquer(ligneNom, LARGEUR_CONTENU));

        String description = creneau.getDescription();
        if (hauteurDisponible > lignes.size() && description != null && !description.isBlank()) {
            lignes.add(tronquer(description, LARGEUR_CONTENU));
        }

        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        int placesRestantes = hauteurDisponible - lignes.size();
        if (placesRestantes > 0 && !fichiers.isEmpty()) {
            boolean troncature = fichiers.size() > placesRestantes;
            int nbAffiches = troncature ? Math.max(0, placesRestantes - 1) : Math.min(placesRestantes, fichiers.size());
            for (int i = 0; i < nbAffiches; i++) {
                Fichier fichier = fichiers.get(i);
                String libelle = (fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank())
                        ? fichier.getNomAffichage() : fichier.getChemin();
                lignes.add(tronquer("· " + libelle, LARGEUR_CONTENU));
            }
            if (troncature) {
                int reste = fichiers.size() - nbAffiches;
                lignes.add(tronquer("+" + reste + " fichier" + (reste > 1 ? "s" : ""), LARGEUR_CONTENU));
            }
        }

        return lignes;
    }

    private static String celluleVide() {
        return "| " + " ".repeat(LARGEUR_CONTENU);
    }

    private static String celluleBoite(char bord, String contenu) {
        return bord + " " + String.format("%-" + LARGEUR_CONTENU + "s", contenu);
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

    private static int clamp(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }
}
