package com.teacherflow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ParametresTest {

    @Test
    void parDefautLesSeptJoursSontAffichesEtLePasEstDeDixMinutes() {
        Parametres parametres = new Parametres();

        assertEquals(7, parametres.getJoursAffiches().size());
        assertEquals(10, parametres.getPasMinutes());
    }

    @Test
    void parDefautDesBlocsHorairesSontDefinis() {
        Parametres parametres = new Parametres();

        assertFalse(parametres.getBlocs().isEmpty());
        assertEquals(LocalTime.of(8, 0), parametres.getBlocs().get(0).getDebut());
    }

    @Test
    void lesBlocsPeuventEtreRemplaces() {
        Parametres parametres = new Parametres();
        PlageHoraire matin = new PlageHoraire(LocalTime.of(9, 0), LocalTime.of(10, 0));
        PlageHoraire aprèsPause = new PlageHoraire(LocalTime.of(10, 20), LocalTime.of(11, 20));

        parametres.setBlocs(List.of(matin, aprèsPause));

        assertEquals(List.of(matin, aprèsPause), parametres.getBlocs());
    }
}
