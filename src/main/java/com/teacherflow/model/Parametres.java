package com.teacherflow.model;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Réglages de l'emploi du temps : quels jours afficher, l'incrément minimal (en minutes)
 * pour le déplacement/redimensionnement des créneaux, et les plages horaires actives par
 * jour (pour représenter des pauses irrégulières — si un jour n'a aucune plage définie,
 * il est considéré actif sur toute l'amplitude affichée).
 */
public class Parametres {

    private static final List<DayOfWeek> JOURS_PAR_DEFAUT = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final int PAS_MINUTES_PAR_DEFAUT = 10;

    private List<DayOfWeek> joursAffiches = new ArrayList<>(JOURS_PAR_DEFAUT);
    private int pasMinutes = PAS_MINUTES_PAR_DEFAUT;
    private Map<DayOfWeek, List<PlageHoraire>> plagesParJour = new LinkedHashMap<>();

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

    public Map<DayOfWeek, List<PlageHoraire>> getPlagesParJour() {
        return plagesParJour;
    }

    public void setPlagesParJour(Map<DayOfWeek, List<PlageHoraire>> plagesParJour) {
        this.plagesParJour = plagesParJour;
    }

    /**
     * @return les plages définies pour ce jour, ou une liste vide si aucune n'est définie
     * (l'appelant doit alors considérer la journée entière comme active).
     */
    public List<PlageHoraire> plagesPour(DayOfWeek jour) {
        return plagesParJour.getOrDefault(jour, List.of());
    }
}
