package com.teacherflow.util;

import java.time.DayOfWeek;
import java.util.Optional;

/**
 * Noms français des jours de la semaine, dans les deux sens (affichage et analyse d'argument CLI).
 */
public final class NomsJours {

    private NomsJours() {
    }

    public static String nom(DayOfWeek jour) {
        return switch (jour) {
            case MONDAY -> "Lundi";
            case TUESDAY -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi";
            case FRIDAY -> "Vendredi";
            case SATURDAY -> "Samedi";
            case SUNDAY -> "Dimanche";
        };
    }

    public static Optional<DayOfWeek> depuisNom(String texte) {
        for (DayOfWeek jour : DayOfWeek.values()) {
            if (nom(jour).equalsIgnoreCase(texte)) {
                return Optional.of(jour);
            }
        }
        return Optional.empty();
    }
}
