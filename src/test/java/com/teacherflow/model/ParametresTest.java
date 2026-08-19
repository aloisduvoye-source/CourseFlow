package com.teacherflow.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParametresTest {

    @Test
    void parDefautLesSeptJoursSontAffichesEtLePasEstDeDixMinutes() {
        Parametres parametres = new Parametres();

        assertEquals(7, parametres.getJoursAffiches().size());
        assertEquals(10, parametres.getPasMinutes());
    }

    @Test
    void unJourSansPlageDefinieRenvoieUneListeVide() {
        Parametres parametres = new Parametres();

        assertTrue(parametres.plagesPour(DayOfWeek.MONDAY).isEmpty());
    }

    @Test
    void plagesPourRenvoieLesPlagesDefinies() {
        Parametres parametres = new Parametres();
        PlageHoraire matin = new PlageHoraire(LocalTime.of(8, 0), LocalTime.of(12, 0));
        parametres.getPlagesParJour().put(DayOfWeek.WEDNESDAY, List.of(matin));

        assertEquals(List.of(matin), parametres.plagesPour(DayOfWeek.WEDNESDAY));
    }
}
