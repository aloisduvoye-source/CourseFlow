package com.teacherflow.cli;

import com.teacherflow.model.Cours;
import com.teacherflow.model.Creneau;
import com.teacherflow.model.EmploiDuTemps;
import com.teacherflow.model.Fichier;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

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

    @Test
    void afficheLaSalleQuandPresente() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setSalle("B204");

        String ligne9h = ligneCommencantPar(GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY), "09:00");

        assertTrue(ligne9h.contains("B204"));
    }

    @Test
    void afficheLaDescriptionSiLaBoiteEstAssezHaute() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), cours.getId());
        creneau.setDescription("Ctrl ch.3");

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertTrue(grille.contains("Ctrl ch.3"));
    }

    @Test
    void neMontrePasLaDescriptionSiLaBoiteNAQuUneLigne() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setDescription("Controle chapitre 3");

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertFalse(grille.contains("Controle chapitre 3"));
    }

    @Test
    void afficheLesFichiersSelectionnesQuandLaBoiteEstAssezHaute() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Fichier fichier = cours.ajouterFichier("/tmp/td1.pdf", "TD1.pdf");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), cours.getId());
        creneau.setFichiersSelectionnesIds(List.of(fichier.getId()));

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertTrue(grille.contains("TD1.pdf"));
    }

    @Test
    void indiqueLeNombreDeFichiersNonAffichesQuandLaBoiteEstTropPetite() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Fichier f1 = cours.ajouterFichier("/tmp/a.pdf", "a.pdf");
        Fichier f2 = cours.ajouterFichier("/tmp/b.pdf", "b.pdf");
        Fichier f3 = cours.ajouterFichier("/tmp/c.pdf", "c.pdf");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), cours.getId());
        creneau.setFichiersSelectionnesIds(List.of(f1.getId(), f2.getId(), f3.getId()));

        String grille = GrilleAscii.construire(emploiDuTemps, DayOfWeek.MONDAY);

        assertTrue(grille.contains("+3 fichiers"));
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
