package com.courseflow.cli;

import com.courseflow.model.Creneau;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Trouve le créneau précédent/suivant un point (jour, heure) donné, en parcourant la semaine
 * de manière circulaire (après le dernier créneau du Dimanche, "suivant" revient au premier
 * créneau du Lundi, et inversement pour "précédent").
 */
public final class NavigationCreneaux {

    private NavigationCreneaux() {
    }

    public static Optional<Creneau> suivant(List<Creneau> creneaux, DayOfWeek jour, LocalTime heure) {
        return relatif(creneaux, jour, heure, true);
    }

    public static Optional<Creneau> precedent(List<Creneau> creneaux, DayOfWeek jour, LocalTime heure) {
        return relatif(creneaux, jour, heure, false);
    }

    private static Optional<Creneau> relatif(List<Creneau> creneaux, DayOfWeek jour, LocalTime heure, boolean suivant) {
        if (creneaux.isEmpty()) {
            return Optional.empty();
        }

        int reference = cle(jour, heure);
        List<Creneau> tries = new ArrayList<>(creneaux);
        tries.sort(Comparator.comparingInt(NavigationCreneaux::cle));

        if (suivant) {
            return tries.stream()
                    .filter(c -> cle(c) > reference)
                    .findFirst()
                    .or(() -> Optional.of(tries.get(0)));
        }

        Collections.reverse(tries);
        return tries.stream()
                .filter(c -> cle(c) < reference)
                .findFirst()
                .or(() -> Optional.of(tries.get(0)));
    }

    private static int cle(Creneau creneau) {
        return cle(creneau.getJour(), creneau.getHeureDebut());
    }

    private static int cle(DayOfWeek jour, LocalTime heure) {
        return jour.getValue() * 24 * 60 + heure.getHour() * 60 + heure.getMinute();
    }
}
