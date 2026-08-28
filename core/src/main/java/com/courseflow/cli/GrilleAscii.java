package com.courseflow.cli;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.TypeSemaine;
import com.courseflow.util.NomsJours;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Construit une représentation ASCII de l'emploi du temps de la semaine : un tableau à
 * bordures (une colonne par jour), où chaque créneau est une boîte (nom, salle, description,
 * fichiers dans la limite de la durée) précédée de sa plage horaire. Les boîtes sont alignées
 * par ordre chronologique au sein de chaque jour (1er créneau du jour sur la même ligne que
 * le 1er des autres jours, etc.), pas par heure absolue.
 */
public final class GrilleAscii {

    private static final DayOfWeek[] JOURS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };
    private static final int LARGEUR_BOITE = 20;
    private static final int LARGEUR_COLONNE = LARGEUR_BOITE + 4;

    private GrilleAscii() {
    }

    /**
     * @param aujourdhui la date à marquer d'un astérisque dans l'en-tête (typiquement la date
     *                    système), qui détermine aussi la semaine (A/B) affichée : seuls les
     *                    créneaux de cette semaine (ou réglés sur "toutes les semaines") sont
     *                    inclus, ces derniers étant repérés par un suffixe "(A)"/"(B)".
     */
    public static String construire(EmploiDuTemps emploiDuTemps, LocalDate aujourdhui) {
        DayOfWeek jourAujourdhui = aujourdhui.getDayOfWeek();
        TypeSemaine semaine = emploiDuTemps.getParametres().semainePour(aujourdhui);

        Map<DayOfWeek, List<Creneau>> parJour = new EnumMap<>(DayOfWeek.class);
        int maxCreneaux = 0;
        for (DayOfWeek jour : JOURS) {
            List<Creneau> creneaux = emploiDuTemps.getCreneaux().stream()
                    .filter(c -> c.getJour() == jour && c.correspondA(semaine))
                    .sorted(Comparator.comparing(Creneau::getHeureDebut))
                    .toList();
            parJour.put(jour, creneaux);
            maxCreneaux = Math.max(maxCreneaux, creneaux.size());
        }

        StringBuilder texte = new StringBuilder();
        texte.append(bordure('┌', '┬', '┐')).append('\n');
        texte.append(ligneEnTetes(jourAujourdhui)).append('\n');
        texte.append(bordure('├', '┼', '┤')).append('\n');

        if (maxCreneaux == 0) {
            texte.append(ligneVide()).append('\n');
        }

        for (int slot = 0; slot < maxCreneaux; slot++) {
            List<List<String>> blocsParJour = new ArrayList<>();
            int hauteurMax = 1;
            for (DayOfWeek jour : JOURS) {
                List<Creneau> creneaux = parJour.get(jour);
                List<String> bloc = slot < creneaux.size()
                        ? construireBlocCreneau(emploiDuTemps, creneaux.get(slot))
                        : List.of();
                blocsParJour.add(bloc);
                hauteurMax = Math.max(hauteurMax, bloc.size());
            }

            for (int ligne = 0; ligne < hauteurMax; ligne++) {
                StringBuilder rangee = new StringBuilder("│");
                for (List<String> bloc : blocsParJour) {
                    String contenu = ligne < bloc.size() ? bloc.get(ligne) : "";
                    rangee.append(' ').append(String.format("%-" + LARGEUR_COLONNE + "s", contenu)).append(' ').append('│');
                }
                texte.append(rangee).append('\n');
            }
            texte.append(ligneVide()).append('\n');
        }

        texte.append(bordure('└', '┴', '┘')).append('\n');
        return texte.toString();
    }

    /**
     * Contenu d'un créneau : plage horaire, puis boîte (nom en majuscules, salle si présente,
     * description si présente, et autant de fichiers que la durée le permet — un fichier par
     * heure de durée, avec un "+N fichiers" si tous ne tiennent pas).
     */
    private static List<String> construireBlocCreneau(EmploiDuTemps emploiDuTemps, Creneau creneau) {
        List<String> lignes = new ArrayList<>();
        String suffixeSemaine = creneau.getTypeSemaine() != TypeSemaine.TOUTES
                ? " (" + creneau.getTypeSemaine() + ")" : "";
        lignes.add(String.format("%-" + LARGEUR_COLONNE + "s",
                creneau.getHeureDebut() + " - " + creneau.getHeureFin() + suffixeSemaine));
        lignes.add("┌" + "─".repeat(LARGEUR_BOITE + 2) + "┐");

        Cours cours = emploiDuTemps.trouverCours(creneau.getCoursId()).orElse(null);
        String nom = (cours != null ? cours.getNom() : "?").toUpperCase();
        lignes.add(ligneBoite(tronquer(nom, LARGEUR_BOITE)));

        String salle = creneau.getSalle();
        if (salle != null && !salle.isBlank()) {
            lignes.add(ligneBoite(tronquer("Salle : " + salle, LARGEUR_BOITE)));
        }
        String description = creneau.getDescription();
        if (description != null && !description.isBlank()) {
            lignes.add(ligneBoite(tronquer(description, LARGEUR_BOITE)));
        }

        int debutMinutes = creneau.getHeureDebut().getHour() * 60 + creneau.getHeureDebut().getMinute();
        int finMinutes = creneau.getHeureFin().getHour() * 60 + creneau.getHeureFin().getMinute();
        int dureeHeures = Math.max(1, (int) Math.ceil((finMinutes - debutMinutes) / 60.0));

        List<Fichier> fichiers = emploiDuTemps.fichiersPourCreneau(creneau);
        int nbAffiches = Math.min(dureeHeures, fichiers.size());
        boolean troncature = fichiers.size() > nbAffiches;
        if (troncature) {
            nbAffiches = Math.max(0, nbAffiches - 1);
        }
        for (int i = 0; i < nbAffiches; i++) {
            Fichier fichier = fichiers.get(i);
            String libelle = (fichier.getNomAffichage() != null && !fichier.getNomAffichage().isBlank())
                    ? fichier.getNomAffichage() : fichier.getChemin();
            lignes.add(ligneBoite(tronquer("· " + libelle, LARGEUR_BOITE)));
        }
        if (troncature) {
            int reste = fichiers.size() - nbAffiches;
            lignes.add(ligneBoite(tronquer("+" + reste + " fichier" + (reste > 1 ? "s" : ""), LARGEUR_BOITE)));
        }

        lignes.add("└" + "─".repeat(LARGEUR_BOITE + 2) + "┘");
        return lignes;
    }

    private static String ligneBoite(String contenuTronque) {
        return "│ " + String.format("%-" + LARGEUR_BOITE + "s", contenuTronque) + " │";
    }

    private static String ligneEnTetes(DayOfWeek aujourdhui) {
        StringBuilder rangee = new StringBuilder("│");
        for (DayOfWeek jour : JOURS) {
            String nom = NomsJours.nom(jour).toUpperCase() + (jour == aujourdhui ? "*" : "");
            rangee.append(' ').append(centrer(nom, LARGEUR_COLONNE)).append(' ').append('│');
        }
        return rangee.toString();
    }

    private static String ligneVide() {
        StringBuilder rangee = new StringBuilder("│");
        for (int i = 0; i < JOURS.length; i++) {
            rangee.append(' ').append(" ".repeat(LARGEUR_COLONNE)).append(' ').append('│');
        }
        return rangee.toString();
    }

    private static String bordure(char gauche, char milieu, char droite) {
        StringBuilder rangee = new StringBuilder();
        rangee.append(gauche);
        for (int i = 0; i < JOURS.length; i++) {
            rangee.append("─".repeat(LARGEUR_COLONNE + 2));
            rangee.append(i < JOURS.length - 1 ? milieu : droite);
        }
        return rangee.toString();
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
