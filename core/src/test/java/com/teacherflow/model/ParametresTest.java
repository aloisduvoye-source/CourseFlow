package com.teacherflow.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParametresTest {

    @Test
    void parDefautLesSeptJoursSontAffichesEtLePasEstDeDixMinutes() {
        Parametres parametres = new Parametres();

        assertEquals(7, parametres.getJoursAffiches().size());
        assertEquals(10, parametres.getPasMinutes());
    }

    @Test
    void parDefautLaGrilleVaDeSeptHeuresAVingtHeures() {
        Parametres parametres = new Parametres();

        assertEquals(LocalTime.of(7, 0), parametres.getHeureDebutGrille());
        assertEquals(LocalTime.of(20, 0), parametres.getHeureFinGrille());
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

    @Test
    void parDefautLeVocabulaireDeTagsContientDmTdCorrectionEtCm() {
        Parametres parametres = new Parametres();

        assertEquals(List.of("dm", "td", "correction", "cm"), parametres.getTagsDisponibles());
    }

    @Test
    void parDefautAucunCoursParDefautNestDesigne() {
        Parametres parametres = new Parametres();

        assertNull(parametres.getCoursDefautId());
    }

    @Test
    void parDefautLeThemeClairEstActif() {
        Parametres parametres = new Parametres();

        assertFalse(parametres.isThemeSombre());
    }

    @Test
    void sansAncrageLaSemaineEstToujoursA() {
        Parametres parametres = new Parametres();

        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2026, 9, 7)));
        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2027, 1, 4)));
    }

    @Test
    void avecAncrageLaSemaineAlterneParParite() {
        Parametres parametres = new Parametres();
        parametres.setAncrageSemaineA(LocalDate.of(2026, 9, 7)); // lundi

        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2026, 9, 7)));
        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2026, 9, 11))); // même semaine
        assertEquals(TypeSemaine.B, parametres.semainePour(LocalDate.of(2026, 9, 14)));
        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2026, 9, 21)));
    }

    @Test
    void avecAncrageLaSemaineAlterneAussiPourDesDatesAnterieures() {
        Parametres parametres = new Parametres();
        parametres.setAncrageSemaineA(LocalDate.of(2026, 9, 21));

        assertEquals(TypeSemaine.B, parametres.semainePour(LocalDate.of(2026, 9, 14)));
        assertEquals(TypeSemaine.A, parametres.semainePour(LocalDate.of(2026, 9, 7)));
    }

    @Test
    void couleurTagRetourneUneCouleurParDefautDeterministeSansPersonnalisation() {
        Parametres parametres = new Parametres();

        String couleur = parametres.couleurTag("dm");

        assertEquals(couleur, parametres.couleurTag("dm"));
    }

    @Test
    void couleurTagUtiliseLaCouleurPersonnaliseeQuandElleExiste() {
        Parametres parametres = new Parametres();

        parametres.getCouleursTags().put("dm", "#123456");

        assertEquals("#123456", parametres.couleurTag("dm"));
    }
}
