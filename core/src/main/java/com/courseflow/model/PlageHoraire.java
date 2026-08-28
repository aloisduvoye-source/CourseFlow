package com.courseflow.model;

import java.time.LocalTime;

/**
 * Une plage horaire (ex. 9h-10h). Un {@link Parametres} définit une liste de plages formant
 * le modèle de blocs de l'emploi du temps (ex. 9h-10h puis 10h20-11h20), reproduit à
 * l'identique sur chaque jour affiché.
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
}
