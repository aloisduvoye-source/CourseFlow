package com.teacherflow.cli;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentsLectureTest {

    @Test
    void sansArgumentUtiliseLesValeursParDefautEtModeCourant() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[0], DayOfWeek.TUESDAY, LocalTime.of(10, 30));

        assertEquals(DayOfWeek.TUESDAY, arguments.getJour());
        assertEquals(LocalTime.of(10, 30), arguments.getHeure());
        assertEquals(ArgumentsLecture.Mode.CRENEAU_COURANT, arguments.getMode());
    }

    @Test
    void optionJourSeuleBasculeEnModeListeJour() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--jour", "Vendredi"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.FRIDAY, arguments.getJour());
        assertEquals(ArgumentsLecture.Mode.LISTE_JOUR, arguments.getMode());
    }

    @Test
    void optionHeureRemplaceLHeureParDefautEtResteEnModeCourant() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--heure", "14:15"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.MONDAY, arguments.getJour());
        assertEquals(LocalTime.of(14, 15), arguments.getHeure());
        assertEquals(ArgumentsLecture.Mode.CRENEAU_COURANT, arguments.getMode());
    }

    @Test
    void combineJourEtHeureResteEnModeCourant() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--jour", "Mercredi", "--heure", "09:00"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertEquals(LocalTime.of(9, 0), arguments.getHeure());
        assertEquals(ArgumentsLecture.Mode.CRENEAU_COURANT, arguments.getMode());
    }

    @Test
    void optionPBasculeEnModePrecedent() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"-p"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Mode.PRECEDENT, arguments.getMode());
    }

    @Test
    void optionNBasculeEnModeSuivant() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"-n"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Mode.SUIVANT, arguments.getMode());
    }

    @Test
    void pEtNEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"-p", "-n"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void parDefautNeListePasLesFichiers() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[0], DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(false, arguments.isListerFichiers());
    }

    @Test
    void optionLActiveLeListageDesFichiers() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"-l"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(true, arguments.isListerFichiers());
        assertEquals(ArgumentsLecture.Mode.CRENEAU_COURANT, arguments.getMode());
    }

    @Test
    void optionLSeCombineAvecPEtN() {
        ArgumentsLecture avecP = ArgumentsLecture.analyser(
                new String[]{"-p", "-l"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));
        assertEquals(ArgumentsLecture.Mode.PRECEDENT, avecP.getMode());
        assertEquals(true, avecP.isListerFichiers());

        ArgumentsLecture avecN = ArgumentsLecture.analyser(
                new String[]{"-n", "-l"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));
        assertEquals(ArgumentsLecture.Mode.SUIVANT, avecN.getMode());
        assertEquals(true, avecN.isListerFichiers());
    }

    @Test
    void optionSBasculeEnModeSemaine() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"-s"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Mode.SEMAINE, arguments.getMode());
    }

    @Test
    void sEtPEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"-s", "-p"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void sEtNEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"-s", "-n"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void jourInconnuLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--jour", "Bricoledi"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void heureInvalideLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--heure", "pas une heure"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void valeurManquanteLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--jour"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    void optionInconnueLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--inconnu"}, DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }
}
