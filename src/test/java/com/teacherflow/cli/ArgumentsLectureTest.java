package com.teacherflow.cli;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgumentsLectureTest {

    @Test
    void sansArgumentUtiliseLesValeursParDefaut() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[0], DayOfWeek.TUESDAY, LocalTime.of(10, 30));

        assertEquals(DayOfWeek.TUESDAY, arguments.getJour());
        assertEquals(LocalTime.of(10, 30), arguments.getHeure());
    }

    @Test
    void optionJourRemplaceLeJourParDefaut() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--jour", "Vendredi"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.FRIDAY, arguments.getJour());
        assertEquals(LocalTime.of(8, 0), arguments.getHeure());
    }

    @Test
    void optionHeureRemplaceLHeureParDefaut() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--heure", "14:15"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.MONDAY, arguments.getJour());
        assertEquals(LocalTime.of(14, 15), arguments.getHeure());
    }

    @Test
    void combineJourEtHeure() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--jour", "Mercredi", "--heure", "09:00"}, DayOfWeek.MONDAY, LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertEquals(LocalTime.of(9, 0), arguments.getHeure());
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
