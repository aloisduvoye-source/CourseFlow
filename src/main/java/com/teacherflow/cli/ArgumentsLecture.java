package com.teacherflow.cli;

import com.teacherflow.util.NomsJours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Analyse des arguments de la commande {@code lecture} : {@code --jour <nom>} et/ou
 * {@code --heure HH:mm} pour cibler un autre créneau, {@code -p}/{@code -n} pour naviguer
 * vers le créneau précédent/suivant. Donner {@code --jour} sans {@code --heure} bascule en
 * mode "liste des créneaux du jour" plutôt que de chercher un créneau précis.
 */
public final class ArgumentsLecture {

    public enum Mode { CRENEAU_COURANT, LISTE_JOUR, PRECEDENT, SUIVANT }

    private final DayOfWeek jour;
    private final LocalTime heure;
    private final Mode mode;

    private ArgumentsLecture(DayOfWeek jour, LocalTime heure, Mode mode) {
        this.jour = jour;
        this.heure = heure;
        this.mode = mode;
    }

    public DayOfWeek getJour() {
        return jour;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public Mode getMode() {
        return mode;
    }

    public static ArgumentsLecture analyser(String[] args, DayOfWeek jourParDefaut, LocalTime heureParDefaut) {
        DayOfWeek jour = jourParDefaut;
        LocalTime heure = heureParDefaut;
        boolean jourSpecifie = false;
        boolean heureSpecifiee = false;
        boolean precedent = false;
        boolean suivant = false;

        int i = 0;
        while (i < args.length) {
            String option = args[i];
            switch (option) {
                case "--jour" -> {
                    String valeur = valeurSuivante(args, i, option);
                    jour = NomsJours.depuisNom(valeur).orElseThrow(() -> new IllegalArgumentException(
                            "Jour inconnu : \"" + valeur + "\" (attendu : Lundi, Mardi, Mercredi, Jeudi, "
                                    + "Vendredi, Samedi ou Dimanche)."));
                    jourSpecifie = true;
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
                    heureSpecifiee = true;
                    i += 2;
                }
                case "-p" -> {
                    precedent = true;
                    i += 1;
                }
                case "-n" -> {
                    suivant = true;
                    i += 1;
                }
                default -> throw new IllegalArgumentException("Option inconnue : \"" + option + "\".");
            }
        }

        if (precedent && suivant) {
            throw new IllegalArgumentException("Les options -p et -n sont incompatibles.");
        }

        Mode mode;
        if (precedent) {
            mode = Mode.PRECEDENT;
        } else if (suivant) {
            mode = Mode.SUIVANT;
        } else if (jourSpecifie && !heureSpecifiee) {
            mode = Mode.LISTE_JOUR;
        } else {
            mode = Mode.CRENEAU_COURANT;
        }

        return new ArgumentsLecture(jour, heure, mode);
    }

    private static String valeurSuivante(String[] args, int index, String option) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException("L'option " + option + " nécessite une valeur.");
        }
        return args[index + 1];
    }
}
