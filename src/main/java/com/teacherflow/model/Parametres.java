package com.teacherflow.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Réglages de l'emploi du temps : quels jours afficher, l'incrément minimal (en minutes)
 * pour le déplacement/redimensionnement des créneaux, et les blocs horaires prédéfinis
 * (ex. 9h-10h puis 10h20-11h20) dans lesquels un nouveau créneau peut être créé. Ce modèle
 * de blocs est identique pour tous les jours affichés.
 */
public class Parametres {

    private static final List<DayOfWeek> JOURS_PAR_DEFAUT = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final int PAS_MINUTES_PAR_DEFAUT = 10;

    private List<DayOfWeek> joursAffiches = new ArrayList<>(JOURS_PAR_DEFAUT);
    private int pasMinutes = PAS_MINUTES_PAR_DEFAUT;
    private List<PlageHoraire> blocs = new ArrayList<>(blocsParDefaut());

    private static List<PlageHoraire> blocsParDefaut() {
        List<PlageHoraire> blocs = new ArrayList<>();
        LocalTime heure = LocalTime.of(8, 0);
        LocalTime fin = LocalTime.of(18, 0);
        while (heure.isBefore(fin)) {
            blocs.add(new PlageHoraire(heure, heure.plusHours(1)));
            heure = heure.plusHours(1);
        }
        return blocs;
    }

    public List<DayOfWeek> getJoursAffiches() {
        return joursAffiches;
    }

    public void setJoursAffiches(List<DayOfWeek> joursAffiches) {
        this.joursAffiches = joursAffiches;
    }

    public int getPasMinutes() {
        return pasMinutes;
    }

    public void setPasMinutes(int pasMinutes) {
        this.pasMinutes = pasMinutes;
    }

    public List<PlageHoraire> getBlocs() {
        return blocs;
    }

    public void setBlocs(List<PlageHoraire> blocs) {
        this.blocs = blocs;
    }
}
