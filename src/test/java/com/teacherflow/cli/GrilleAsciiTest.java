package com.teacherflow.cli;

import com.teacherflow.model.Cours;
import com.teacherflow.model.EmploiDuTemps;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrilleAsciiTest {

    @Test
    void contientLesEnTetesDesSeptJours() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertTrue(grille.contains("Lundi"));
        assertTrue(grille.contains("Mardi"));
        assertTrue(grille.contains("Mercredi"));
        assertTrue(grille.contains("Jeudi"));
        assertTrue(grille.contains("Vendredi"));
        assertTrue(grille.contains("Samedi"));
        assertTrue(grille.contains("Dimanche"));
    }

    @Test
    void marqueLeJourCourantDUnAsterisque() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.WEDNESDAY);

        assertTrue(grille.contains("Mercredi*"));
    }

    @Test
    void afficheLeNomDuCoursALHeureDuCreneau() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        emploiDuTemps.ajouterCreneau(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());

        String ligne9h = ligneCommencantPar(GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY), "09:00");

        assertTrue(ligne9h.contains("6e A"));
    }

    @Test
    void neContientPasLeCoursEnDehorsDeSonCreneau() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        emploiDuTemps.ajouterCreneau(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertFalse(ligneCommencantPar(grille, "07:00").contains("6e A"));
        assertFalse(ligneCommencantPar(grille, "11:00").contains("6e A"));
    }

    private static String ligneCommencantPar(String texte, String prefixe) {
        for (String ligne : texte.split("\n")) {
            if (ligne.startsWith(prefixe)) {
                return ligne;
            }
        }
        throw new AssertionError("Aucune ligne commençant par \"" + prefixe + "\"");
    }
}
