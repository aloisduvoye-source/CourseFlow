package com.teacherflow.cli;

import com.teacherflow.util.NomsJours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Analyse des arguments de la commande {@code lecture} : {@code --jour <nom>} et/ou
 * {@code --heure HH:mm} pour cibler un autre créneau que celui du moment présent.
 */
public final class ArgumentsLecture {

    private final DayOfWeek jour;
    private final LocalTime heure;

    private ArgumentsLecture(DayOfWeek jour, LocalTime heure) {
        this.jour = jour;
        this.heure = heure;
    }

    public DayOfWeek getJour() {
        return jour;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public static ArgumentsLecture analyser(String[] args, DayOfWeek jourParDefaut, LocalTime heureParDefaut) {
        DayOfWeek jour = jourParDefaut;
        LocalTime heure = heureParDefaut;

        int i = 0;
        while (i < args.length) {
            String option = args[i];
            switch (option) {
                case "--jour" -> {
                    String valeur = valeurSuivante(args, i, option);
                    jour = NomsJours.depuisNom(valeur).orElseThrow(() -> new IllegalArgumentException(
                            "Jour inconnu : \"" + valeur + "\" (attendu : Lundi, Mardi, Mercredi, Jeudi, "
                                    + "Vendredi, Samedi ou Dimanche)."));
                    i += 2;
                }
                case "--heure" -> {
                    String valeur = valeurSuivante(args, i, option);
                    try {
                        heure = LocalTime.parse(valeur);
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException(
                                "Heure invalide : \"" + valeur + "\" (format attendu : HH:mm).");
                    }
                    i += 2;
                }
                default -> throw new IllegalArgumentException("Option inconnue : \"" + option + "\".");
            }
        }

        return new ArgumentsLecture(jour, heure);
    }

    private static String valeurSuivante(String[] args, int index, String option) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException("L'option " + option + " nécessite une valeur.");
        }
        return args[index + 1];
    }
}
