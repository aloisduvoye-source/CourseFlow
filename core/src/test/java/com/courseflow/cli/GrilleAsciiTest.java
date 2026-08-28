package com.courseflow.cli;

import com.courseflow.model.Cours;
import com.courseflow.model.Creneau;
import com.courseflow.model.EmploiDuTemps;
import com.courseflow.model.Fichier;
import com.courseflow.model.TypeSemaine;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrilleAsciiTest {

    @Test
    void contientLesEnTetesDesSeptJours() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("LUNDI"));
        assertTrue(grille.contains("MARDI"));
        assertTrue(grille.contains("MERCREDI"));
        assertTrue(grille.contains("JEUDI"));
        assertTrue(grille.contains("VENDREDI"));
        assertTrue(grille.contains("SAMEDI"));
        assertTrue(grille.contains("DIMANCHE"));
    }

    @Test
    void marqueLeJourCourantDUnAsterisque() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 26));

        assertTrue(grille.contains("MERCREDI*"));
    }

    @Test
    void afficheLeNomDuCoursEnMajuscules() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        emploiDuTemps.ajouterCreneau(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("6E A"));
    }

    @Test
    void afficheLaPlageHoraire() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        emploiDuTemps.ajouterCreneau(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("09:00 - 10:00"));
    }

    @Test
    void afficheLaSalleQuandPresente() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setSalle("B204");

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("Salle : B204"));
    }

    @Test
    void afficheLaDescriptionQuandPresenteMemeSurUneHeure() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setDescription("Ctrl ch.3");

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("Ctrl ch.3"));
    }

    @Test
    void afficheAuMoinsUnFichierParHeureDeDuree() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Fichier fichier = cours.ajouterFichier("/tmp/td1.pdf", "TD1.pdf");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setFichiersSelectionnesIds(List.of(fichier.getId()));

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("TD1.pdf"));
    }

    @Test
    void indiqueLeNombreDeFichiersNonAffichesQuandLaDureeEstTropCourte() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Fichier f1 = cours.ajouterFichier("/tmp/a.pdf", "a.pdf");
        Fichier f2 = cours.ajouterFichier("/tmp/b.pdf", "b.pdf");
        Fichier f3 = cours.ajouterFichier("/tmp/c.pdf", "c.pdf");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneau.setFichiersSelectionnesIds(List.of(f1.getId(), f2.getId(), f3.getId()));

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("+3 fichiers"));
    }

    @Test
    void alignePlusieursCreneauxDuMemeJourSurDesLignesDistinctes() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        emploiDuTemps.ajouterCreneau(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), cours.getId());
        emploiDuTemps.ajouterCreneau(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), cours.getId());

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 30));

        assertTrue(grille.contains("08:00 - 09:00"));
        assertTrue(grille.contains("10:00 - 11:00"));
    }

    @Test
    void omitLesCreneauxDeLAutreSemaineEtSuffixeCeuxAffiches() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours cours = emploiDuTemps.ajouterCours("6e A", "#3498db");
        Creneau creneauB = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), cours.getId());
        creneauB.setTypeSemaine(TypeSemaine.B);
        emploiDuTemps.getParametres().setAncrageSemaineA(LocalDate.of(2026, 8, 24)); // semaine du 24/08 = A

        String grilleSemaineA = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));
        assertFalse(grilleSemaineA.contains("09:00 - 10:00"));

        String grilleSemaineB = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 31));
        assertTrue(grilleSemaineB.contains("09:00 - 10:00 (B)"));
    }

    @Test
    void neCrashePasQuandLEmploiDuTempsEstVide() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        String grille = GrilleAscii.construire(emploiDuTemps, LocalDate.of(2026, 8, 24));

        assertTrue(grille.contains("LUNDI"));
    }
}
