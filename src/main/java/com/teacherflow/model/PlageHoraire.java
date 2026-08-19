package com.teacherflow.model;

import java.time.LocalTime;

/**
 * Une plage horaire active pour un jour donné (ex. 8h-12h). Un {@link Parametres} peut
 * définir plusieurs plages par jour pour représenter des pauses irrégulières (ex. 8h-12h
 * puis 13h30-17h, avec une pause déjeuner entre les deux).
 */
public class PlageHoraire {

    private LocalTime debut;
    private LocalTime fin;

    public PlageHoraire() {
    }

    public PlageHoraire(LocalTime debut, LocalTime fin) {
        this.debut = debut;
        this.fin = fin;
    }

    public LocalTime getDebut() {
        return debut;
    }

    public void setDebut(LocalTime debut) {
        this.debut = debut;
    }

    public LocalTime getFin() {
        return fin;
    }

    public void setFin(LocalTime fin) {
        this.fin = fin;
    }

    /**
     * @return true si l'heure donnée tombe dans cette plage (borne de fin exclue).
     */
    public boolean contient(LocalTime heure) {
        return !heure.isBefore(debut) && heure.isBefore(fin);
    }
}
