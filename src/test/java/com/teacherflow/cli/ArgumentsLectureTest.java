package com.teacherflow.cli;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentsLectureTest {

    @Test
    void sansArgumentUtiliseLesValeursParDefautEtCommandeOuvrir() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[0], LocalDate.of(2026, 8, 25), LocalTime.of(10, 30));

        assertEquals(ArgumentsLecture.Commande.OUVRIR, arguments.getCommande());
        assertEquals(DayOfWeek.TUESDAY, arguments.getJour());
        assertEquals(LocalTime.of(10, 30), arguments.getHeure());
    }

    @Test
    void optionNextBasculeEnModeSuivant() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--next"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertTrue(arguments.isSuivant());
    }

    @Test
    void optionPreviousBasculeEnModePrecedent() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--previous"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertTrue(arguments.isPrecedent());
    }

    @Test
    void nextEtPreviousEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--next", "--previous"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void optionDayEtTimeCiblentUnCreneauPrecis() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--day", "mercredi", "--time", "09:00"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertEquals(LocalTime.of(9, 0), arguments.getHeure());
        assertEquals(ArgumentsLecture.Commande.OUVRIR, arguments.getCommande());
    }

    @Test
    void optionDateResoutLeJourDeLaSemaine() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--date", "2026-08-19", "--time", "10:00"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertEquals(LocalTime.of(10, 0), arguments.getHeure());
        assertEquals(LocalDate.of(2026, 8, 19), arguments.getDate());
    }

    @Test
    void optionDayAjusteLeJourDansLaSemaineDeLaDateParDefaut() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--day", "mercredi", "--time", "09:00"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(LocalDate.of(2026, 8, 26), arguments.getDate());
    }

    @Test
    void dayEtDateEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"--day", "lundi", "--date", "2026-08-19", "--time", "10:00"},
                LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void dayOuDateSansTimeLeveUneErreurPourOuvrirEtSlot() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"--day", "mercredi"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"slot", "--date", "2026-08-19"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void daySansTimeResteValideAvecNextOuPrevious() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"--next", "--day", "mercredi"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertTrue(arguments.isSuivant());
    }

    @Test
    void jourInconnuLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--day", "Bricoledi", "--time", "10:00"},
                        LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void dateInvalideLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--date", "pas une date", "--time", "10:00"},
                        LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void heureInvalideLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--time", "pas une heure"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void valeurManquanteLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--day"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void optionInconnueLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"--inconnu"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void commandeInconnueLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"bricole"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeSlotEstReconnue() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"slot"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Commande.SLOT, arguments.getCommande());
    }

    @Test
    void sousCommandeSlotsEstReconnueEtNAcceptePasTime() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"slots", "--day", "vendredi"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));
        assertEquals(ArgumentsLecture.Commande.SLOTS, arguments.getCommande());
        assertEquals(DayOfWeek.FRIDAY, arguments.getJour());

        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"slots", "--time", "10:00"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"slots", "--next"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeScheduleNAccepteAucuneOption() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"schedule"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));
        assertEquals(ArgumentsLecture.Commande.SCHEDULE, arguments.getCommande());

        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"schedule", "--next"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeCoursesEtMissingInfo() {
        ArgumentsLecture sansFiltre = ArgumentsLecture.analyser(
                new String[]{"courses"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));
        assertEquals(ArgumentsLecture.Commande.COURSES, sansFiltre.getCommande());
        assertFalse(sansFiltre.isMissingInfo());

        ArgumentsLecture avecFiltre = ArgumentsLecture.analyser(
                new String[]{"courses", "--missing-info"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));
        assertTrue(avecFiltre.isMissingInfo());
    }

    @Test
    void sousCommandeCourseAttendUnNomPositionnel() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"course", "Mathématiques"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Commande.COURSE, arguments.getCommande());
        assertEquals("Mathématiques", arguments.getNomCours());
    }

    @Test
    void sousCommandeCourseSansNomLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () ->
                ArgumentsLecture.analyser(new String[]{"course"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeOpenFileAvecCourseEtFile() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"open-file", "--course", "Maths", "--file", "chapitre1.pdf"},
                LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(ArgumentsLecture.Commande.OPEN_FILE, arguments.getCommande());
        assertEquals("Maths", arguments.getNomCours());
        assertEquals("chapitre1.pdf", arguments.getNomFichier());
    }

    @Test
    void sousCommandeOpenFileAvecDayEtTime() {
        ArgumentsLecture arguments = ArgumentsLecture.analyser(
                new String[]{"open-file", "--day", "mercredi", "--time", "10:00", "--file", "cours.pdf"},
                LocalDate.of(2026, 8, 24), LocalTime.of(8, 0));

        assertEquals(DayOfWeek.WEDNESDAY, arguments.getJour());
        assertNull(arguments.getNomCours());
        assertEquals("cours.pdf", arguments.getNomFichier());
    }

    @Test
    void sousCommandeOpenFileSansFileLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"open-file", "--course", "Maths"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeOpenFileSansSourceLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"open-file", "--file", "cours.pdf"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeOpenFileCourseEtDayEnsembleLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"open-file", "--course", "Maths", "--day", "lundi", "--time", "08:00", "--file", "x.pdf"},
                LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }

    @Test
    void sousCommandeOpenFileDaySansTimeLeveUneErreur() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentsLecture.analyser(
                new String[]{"open-file", "--day", "lundi", "--file", "x.pdf"}, LocalDate.of(2026, 8, 24), LocalTime.of(8, 0)));
    }
}
