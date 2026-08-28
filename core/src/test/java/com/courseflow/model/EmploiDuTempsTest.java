package com.courseflow.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmploiDuTempsTest {

    @Test
    void unCreneauNeResoutQueLesFichiersSelectionnesParmiCeuxDuCours() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();

        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier cours = maths.ajouterFichier("/docs/maths/cours-fractions.pdf", "Cours fractions");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices-fractions.pdf", "Exercices");
        maths.ajouterFichier("/docs/maths/corrige-ds1.pdf", "Corrigé DS1");

        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), maths.getId());
        creneau.selectionnerFichier(cours.getId());
        creneau.selectionnerFichier(exercices.getId());

        List<Fichier> fichiersDuCreneau = emploiDuTemps.fichiersPourCreneau(creneau);

        assertEquals(2, fichiersDuCreneau.size());
        assertTrue(fichiersDuCreneau.contains(cours));
        assertTrue(fichiersDuCreneau.contains(exercices));
    }

    @Test
    void trouverCreneauCourantRepereLeCreneauActifAJourEtHeureDonnes() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours histoire = emploiDuTemps.ajouterCours("3e B - Histoire", "#e67e22");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), histoire.getId());

        LocalDate unMardi = LocalDate.of(2026, 8, 25);
        LocalDate unMercredi = LocalDate.of(2026, 8, 26);

        assertTrue(emploiDuTemps.trouverCreneauCourant(unMardi, LocalTime.of(10, 30))
                .filter(c -> c.getId().equals(creneau.getId()))
                .isPresent());

        assertTrue(emploiDuTemps.trouverCreneauCourant(unMardi, LocalTime.of(11, 0)).isEmpty());
        assertTrue(emploiDuTemps.trouverCreneauCourant(unMercredi, LocalTime.of(10, 30)).isEmpty());
    }

    @Test
    void trouverCreneauCourantRespecteLaSemaineAlterneeDuCreneau() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours histoire = emploiDuTemps.ajouterCours("3e B - Histoire", "#e67e22");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), histoire.getId());
        creneau.setTypeSemaine(TypeSemaine.B);
        emploiDuTemps.getParametres().setAncrageSemaineA(LocalDate.of(2026, 8, 24)); // lundi = semaine A

        LocalDate mardiSemaineA = LocalDate.of(2026, 8, 25);
        LocalDate mardiSemaineB = LocalDate.of(2026, 9, 1);

        assertTrue(emploiDuTemps.trouverCreneauCourant(mardiSemaineA, LocalTime.of(10, 30)).isEmpty());
        assertTrue(emploiDuTemps.trouverCreneauCourant(mardiSemaineB, LocalTime.of(10, 30))
                .filter(c -> c.getId().equals(creneau.getId()))
                .isPresent());
    }

    @Test
    void supprimerUnCoursSupprimeAussiSesCreneaux() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), maths.getId());

        emploiDuTemps.supprimerCours(maths.getId());

        assertTrue(emploiDuTemps.trouverCours(maths.getId()).isEmpty());
        assertTrue(emploiDuTemps.trouverCreneau(creneau.getId()).isEmpty());
    }

    @Test
    void fichiersVisiblesInclutLesFichiersProduitsEtLiesDepuisLeCoursParDefaut() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices.pdf", "Exercices");

        Cours soutien = emploiDuTemps.ajouterCours("Soutien scolaire", "#e67e22");
        Fichier fiche = soutien.ajouterFichier("/docs/soutien/fiche.pdf", "Fiche");
        soutien.ajouterFichierLie(exercices.getId());

        List<Fichier> visibles = emploiDuTemps.fichiersVisibles(soutien);

        assertEquals(2, visibles.size());
        assertTrue(visibles.contains(fiche));
        assertTrue(visibles.contains(exercices));
    }

    @Test
    void fichiersVisiblesIgnoreUnLienVersUnFichierSupprime() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices.pdf", "Exercices");

        Cours soutien = emploiDuTemps.ajouterCours("Soutien scolaire", "#e67e22");
        soutien.ajouterFichierLie(exercices.getId());
        maths.retirerFichier(exercices.getId());

        List<Fichier> visibles = emploiDuTemps.fichiersVisibles(soutien);

        assertTrue(visibles.isEmpty());
    }

    @Test
    void unCreneauPeutResoudreUnFichierLieDepuisLeCoursParDefaut() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");
        Fichier exercices = maths.ajouterFichier("/docs/maths/exercices.pdf", "Exercices");

        Cours soutien = emploiDuTemps.ajouterCours("Soutien scolaire", "#e67e22");
        soutien.ajouterFichierLie(exercices.getId());

        Creneau creneau = emploiDuTemps.ajouterCreneau(
                DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), soutien.getId());
        creneau.selectionnerFichier(exercices.getId());

        List<Fichier> fichiersDuCreneau = emploiDuTemps.fichiersPourCreneau(creneau);

        assertEquals(1, fichiersDuCreneau.size());
        assertTrue(fichiersDuCreneau.contains(exercices));
    }

    @Test
    void trouverCoursDefautResoutLIdEnregistreDansLesParametres() {
        EmploiDuTemps emploiDuTemps = new EmploiDuTemps();
        Cours maths = emploiDuTemps.ajouterCours("6e A - Mathématiques", "#3498db");

        assertTrue(emploiDuTemps.trouverCoursDefaut().isEmpty());

        emploiDuTemps.getParametres().setCoursDefautId(maths.getId());

        assertEquals(maths, emploiDuTemps.trouverCoursDefaut().orElseThrow());
    }
}
